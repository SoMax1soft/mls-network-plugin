// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.http.TransportFailure;
import italy.mlsac.net.backend.wire.WireOffer;
import italy.mlsac.net.json.JsonObject;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class SessionOpener {

    private static final String SESSION_FALLBACK_PREFIX = "http-session-";

    private final BackendContext context;

    SessionOpener(BackendContext context) {
        this.context = context;
    }

    CompletableFuture<OpenOutcome> attempt() {
        return openOn(Endpoint.PRIMARY).thenCompose(outcome -> {
            if (outcome instanceof OpenOutcome.Rejected rejected) {
                return afterPrimaryRejected(rejected);
            }
            return CompletableFuture.completedFuture(outcome);
        });
    }

    CompletableFuture<OpenOutcome> openOn(Endpoint endpoint) {
        byte[] body = context.bodies().init(context.identity(), context.accessKey(),
                context.connectedPlayers().getAsInt(), WireOffer.HIGHEST_VERSION, context.environment());
        return context.post(endpoint, "init", false, body).handle((reply, error) ->
                error != null ? rejectedByTransport(error) : fromReply(reply, endpoint));
    }

    private CompletableFuture<OpenOutcome> afterPrimaryRejected(OpenOutcome.Rejected rejected) {
        switch (rejected.kind()) {
            case AUTH -> context.console().authFailure(rejected.reason());
            case OTHER -> context.console().cause("init failed: " + rejected.reason());
            case SWITCHABLE -> {
                if (context.address().reserveDistinct()) {
                    context.console().tryingReserve(context.address().primary(), rejected.reason(),
                            context.address().base(Endpoint.RESERVE));
                    return openOn(Endpoint.RESERVE).thenApply(this::reportReserveRejection);
                }
                context.console().debug("connection failed: " + rejected.reason());
            }
        }
        return CompletableFuture.completedFuture(rejected);
    }

    private OpenOutcome reportReserveRejection(OpenOutcome outcome) {
        if (outcome instanceof OpenOutcome.Rejected rejected) {
            if (rejected.kind() == OpenOutcome.RejectionKind.AUTH) {
                context.console().authFailure(rejected.reason());
            } else {
                context.console().debug("reserve connection failed: " + rejected.reason());
            }
        }
        return outcome;
    }

    private OpenOutcome rejectedByTransport(Throwable error) {
        TransportFailure failure = TransportFailure.of(error);
        OpenOutcome.RejectionKind kind = failure.kind().unreachable()
                ? OpenOutcome.RejectionKind.SWITCHABLE : OpenOutcome.RejectionKind.OTHER;
        return new OpenOutcome.Rejected(kind, failure.kind().name().toLowerCase(Locale.ROOT)
                + " (" + failure.getMessage() + ")");
    }

    private OpenOutcome fromReply(HttpReply reply, Endpoint endpoint) {
        Optional<JsonObject> json = LenientJson.object(reply.text());
        json.ifPresent(context.replies()::warnings);
        StatusClass status = StatusClass.of(reply.status());
        String reason = "HTTP " + reply.status();
        return switch (status) {
            case SUCCESS -> new OpenOutcome.Opened(endpoint, sessionIdOf(json), WireOffer.from(json));
            case AUTH -> new OpenOutcome.Rejected(OpenOutcome.RejectionKind.AUTH, reason);
            case NO_SUCH_ENDPOINT, BACKEND_FAULT -> new OpenOutcome.Rejected(OpenOutcome.RejectionKind.SWITCHABLE, reason);
            default -> new OpenOutcome.Rejected(OpenOutcome.RejectionKind.OTHER, reason);
        };
    }

    private String sessionIdOf(Optional<JsonObject> json) {
        Optional<String> root = json.flatMap(o -> o.string("sessionId")).filter(s -> !s.isEmpty());
        Optional<String> nested = json.flatMap(o -> o.object("data")).flatMap(d -> d.string("sessionId"))
                .filter(s -> !s.isEmpty());
        return root.or(() -> nested)
                .orElseGet(() -> SESSION_FALLBACK_PREFIX + context.clock().wallMillis("http.session_fallback"));
    }
}
