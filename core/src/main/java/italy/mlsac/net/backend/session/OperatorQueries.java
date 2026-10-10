// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.LatencyResult;
import italy.mlsac.net.backend.TodayStats;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class OperatorQueries {

    static final int LATENCY_PROBES = 5;

    private final BackendContext context;
    private final ConnectionState state;

    OperatorQueries(BackendContext context, ConnectionState state) {
        this.context = context;
        this.state = state;
    }

    CompletableFuture<LatencyResult> probeLatency() {
        List<CompletableFuture<Long>> probes = new ArrayList<>();
        for (int i = 0; i < LATENCY_PROBES; i++) {
            byte[] body = context.bodies().status(context.identity(), state.sessionKey(),
                    context.connectedPlayers().getAsInt());
            probes.add(context.post(state.current(), "heartbeat", true, body)
                    .handle((reply, error) -> error == null && reply.success() ? reply.headerMillis() : 0L));
        }
        return CompletableFuture.allOf(probes.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> LatencyResult.of(probes.stream().map(CompletableFuture::join).toList(),
                        LATENCY_PROBES));
    }

    CompletableFuture<Optional<TodayStats>> todayStats() {
        return context.post(state.current(), "stats/today", true, context.bodies().emptyObject())
                .handle((reply, error) -> error == null ? parse(reply) : Optional.empty());
    }

    private static Optional<TodayStats> parse(HttpReply reply) {
        if (!reply.success()) {
            return Optional.empty();
        }
        return LenientJson.object(reply.text())
                .filter(root -> root.bool("success").orElse(false))
                .flatMap(OperatorQueries::toStats);
    }

    private static Optional<TodayStats> toStats(JsonObject root) {
        Optional<JsonObject> requests = root.object("requests");
        Optional<Long> detections = number(root, "detections");
        if (requests.isEmpty() || detections.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TodayStats(root.string("date").orElse(""),
                number(requests.get(), "used").orElse(0L),
                number(requests.get(), "limit").orElse(0L),
                number(requests.get(), "remaining").orElse(0L),
                detections.get()));
    }

    private static Optional<Long> number(JsonObject object, String name) {
        return object.number(name).map(JsonNumber::doubleValue).map(Math::round);
    }
}
