// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.json.JsonObject;
import java.util.Optional;

final class OnlineExchange {

    private final BackendContext context;
    private final ConnectionState state;
    private final DegradedModes degraded;

    OnlineExchange(BackendContext context, ConnectionState state, DegradedModes degraded) {
        this.context = context;
        this.state = state;
        this.degraded = degraded;
    }

    void run() {
        if (state.stopping()) {
            return;
        }
        Endpoint target = state.current();
        byte[] body = context.bodies().status(context.identity(), state.sessionKey(),
                context.connectedPlayers().getAsInt(), context.clients().get());
        context.post(target, "online", true, body).whenComplete((reply, error) -> {
            if (state.stopping()) {
                return;
            }
            if (error != null) {
                degraded.networkFailure("reportstats", target);
                return;
            }
            handleOnline(reply, target, body);
        });
    }

    private void handleOnline(HttpReply reply, Endpoint target, byte[] body) {
        degraded.answered();
        Optional<JsonObject> json = LenientJson.object(reply.text());
        json.ifPresent(context.replies()::warnings);
        StatusClass status = StatusClass.of(reply.status());
        switch (status) {
            case SUCCESS -> {
                json.ifPresent(context.replies()::interServerEvents);
                degraded.clearServerErrorAndQuota();
            }
            case NO_SUCH_ENDPOINT, METHOD_NOT_ALLOWED -> sendLegacyStats(target, body);
            case QUOTA -> degraded.quotaExceeded();
            case BACKEND_FAULT -> degraded.backendFault("online HTTP " + reply.status(), target);
            default -> {
            }
        }
    }

    private void sendLegacyStats(Endpoint target, byte[] body) {
        context.post(target, "reportstats", true, body).whenComplete((reply, error) -> {
            if (state.stopping()) {
                return;
            }
            if (error != null) {
                degraded.networkFailure("reportstats", target);
                return;
            }
            degraded.answered();
            LenientJson.object(reply.text()).ifPresent(context.replies()::warnings);
            switch (StatusClass.of(reply.status())) {
                case SUCCESS -> degraded.clearServerErrorAndQuota();
                case QUOTA -> degraded.quotaExceeded();
                case NO_SUCH_ENDPOINT -> degraded.notFound("reportstats HTTP 404", target);
                case BACKEND_FAULT -> degraded.backendFault("reportstats HTTP " + reply.status(), target);
                default -> {
                }
            }
        });
    }
}
