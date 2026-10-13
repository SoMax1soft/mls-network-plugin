// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.VerdictListener;
import italy.mlsac.net.backend.http.FailureKind;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.http.TransportFailure;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.wire.PredictRequest;
import italy.mlsac.net.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;

final class PredictChannel {

    static final int MAX_IN_FLIGHT = 32;
    private static final String SESSION_UNKNOWN = "SESSION_UNKNOWN";

    private final BackendContext context;
    private final ConnectionState state;
    private final DegradedModes degraded;
    private final PredictStreamReader reader;
    private final WaitingWindows waiting = new WaitingWindows();

    PredictChannel(BackendContext context, ConnectionState state, DegradedModes degraded) {
        this.context = context;
        this.state = state;
        this.degraded = degraded;
        this.reader = new PredictStreamReader(context.replies());
    }

    CompletableFuture<PredictOutcome> predict(WindowSubmission request, VerdictListener listener) {
        Optional<PredictOutcome.RefusalReason> refusal = refusal();
        if (refusal.isPresent()) {
            context.console().debug("predict refused: " + refusal.get());
            waiting.settleAll(new PredictOutcome.Refused(refusal.get()));
            return CompletableFuture.completedFuture(new PredictOutcome.Refused(refusal.get()));
        }
        if (!state.claimPredict(MAX_IN_FLIGHT)) {
            CompletableFuture<PredictOutcome> parked = waiting.park(request, listener);
            drain();
            return parked;
        }
        return start(request, listener);
    }

    private void drain() {
        while (!waiting.empty() && state.claimPredict(MAX_IN_FLIGHT)) {
            Optional<WaitingWindows.Parked> next = waiting.next();
            if (next.isEmpty()) {
                state.releasePredict();
                return;
            }
            WaitingWindows.Parked parked = next.get();
            Optional<PredictOutcome.RefusalReason> refusal = refusal();
            if (refusal.isPresent()) {
                state.releasePredict();
                parked.outcome().complete(new PredictOutcome.Refused(refusal.get()));
                continue;
            }
            start(parked.request(), parked.listener()).whenComplete((outcome, failure) -> {
                if (failure != null) {
                    parked.outcome().completeExceptionally(failure);
                } else {
                    parked.outcome().complete(outcome);
                }
            });
        }
    }

    private CompletableFuture<PredictOutcome> start(WindowSubmission request, VerdictListener listener) {
        Endpoint target = state.current();
        PredictRoute route = state.predictRoute();
        try {
            return CompletableFuture.supplyAsync(() -> send(target, route, request, listener), context.encoder())
                    .thenCompose(Function.identity());
        } catch (RejectedExecutionException stopped) {
            state.releasePredict();
            drain();
            return CompletableFuture.completedFuture(new PredictOutcome.Failed("Canceled"));
        }
    }

    private CompletableFuture<PredictOutcome> send(Endpoint target, PredictRoute route, WindowSubmission request,
                                                   VerdictListener listener) {
        CompletableFuture<HttpReply> reply;
        try {
            PredictRequest call = route.wire().request(context.bodies(), context.identity(), route.sessionKey(),
                    request);
            reply = context.post(target, "predict-stream", true, call.body(), call.extraHeaders());
        } catch (RuntimeException failure) {
            state.releasePredict();
            drain();
            context.console().debug("predict window could not be encoded: " + failure);
            return CompletableFuture.completedFuture(new PredictOutcome.Failed("encoding: " + failure.getMessage()));
        }
        return reply.handle((answer, error) -> {
            try {
                return error != null ? onTransportFailure(error, target) : onReply(answer, target, route, listener);
            } finally {
                state.releasePredict();
                drain();
            }
        });
    }

    private Optional<PredictOutcome.RefusalReason> refusal() {
        if (state.stopping() || !state.connected()) {
            return Optional.of(PredictOutcome.RefusalReason.NOT_CONNECTED);
        }
        if (state.stasis()) {
            return Optional.of(PredictOutcome.RefusalReason.STASIS);
        }
        if (state.quotaExceeded()) {
            return Optional.of(PredictOutcome.RefusalReason.QUOTA);
        }
        if (degraded.serverErrorActive()) {
            return Optional.of(PredictOutcome.RefusalReason.BACKEND_FAULT);
        }
        return Optional.empty();
    }

    private PredictOutcome onTransportFailure(Throwable error, Endpoint target) {
        TransportFailure failure = TransportFailure.of(error);
        if (state.stopping() || failure.kind() == FailureKind.CANCELLED) {
            return new PredictOutcome.Failed("Canceled");
        }
        degraded.networkFailure("predict", target);
        DebugTally.count("backend.transport-failure." + failure.kind());
        return new PredictOutcome.Failed("transport: " + failure.kind());
    }

    private PredictOutcome onReply(HttpReply reply, Endpoint target, PredictRoute route, VerdictListener listener) {
        if (state.stopping()) {
            return new PredictOutcome.Failed("Canceled");
        }
        degraded.answered();
        DebugTally.count("backend.http." + reply.status());
        StatusClass status = StatusClass.of(reply.status());
        if (status == StatusClass.SUCCESS) {
            PredictStreamReader.Result result = reader.read(reply.text(), listener);
            return result.failure().<PredictOutcome>map(PredictOutcome.Failed::new)
                    .orElseGet(() -> new PredictOutcome.Completed(result.delivered()));
        }
        Optional<JsonObject> answer = LenientJson.object(reply.text());
        answer.ifPresent(context.replies()::warnings);
        String reason = "predict HTTP " + reply.status();
        if (status == StatusClass.CONFLICT && answer.map(PredictChannel::asksForNewSession).orElse(false)) {
            degraded.sessionUnknown(route);
            return new PredictOutcome.Failed(reason + " (session unknown)");
        }
        switch (status) {
            case NO_SUCH_ENDPOINT, METHOD_NOT_ALLOWED -> {
                context.console().cause("backend does not support predict-stream - update the inference backend");
                if (status == StatusClass.NO_SUCH_ENDPOINT) {
                    degraded.notFound(reason, target);
                }
            }
            case AUTH -> {
                context.console().authFailure(reason);
                degraded.markDisconnected();
            }
            case QUOTA -> degraded.quotaExceeded();
            case BACKEND_FAULT -> {
                degraded.backendFault(reason, target);
                degraded.backendFault(reason, target);
            }
            default -> context.console().cause("predict refused: " + reason);
        }
        return new PredictOutcome.Failed(reason);
    }

    private static boolean asksForNewSession(JsonObject answer) {
        boolean reinit = answer.bool("reinit").orElse(false);
        boolean unknown = answer.object("error").flatMap(error -> error.string("code"))
                .filter(SESSION_UNKNOWN::equals).isPresent();
        return reinit || unknown;
    }
}
