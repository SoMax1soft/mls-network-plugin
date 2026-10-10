// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpReply;

final class HeartbeatExchange {

    private final BackendContext context;
    private final ConnectionState state;
    private final DegradedModes degraded;
    private final EndpointFailover failover;

    HeartbeatExchange(BackendContext context, ConnectionState state, DegradedModes degraded, EndpointFailover failover) {
        this.context = context;
        this.state = state;
        this.degraded = degraded;
        this.failover = failover;
    }

    void run() {
        if (state.stopping()) {
            return;
        }
        failover.probePrimaryIfDue();
        Endpoint target = state.current();
        byte[] body = context.bodies().status(context.identity(), state.sessionKey(),
                context.connectedPlayers().getAsInt(), context.clients().get());
        context.post(target, "heartbeat", true, body).whenComplete((reply, error) -> {
            if (state.stopping()) {
                return;
            }
            if (error != null) {
                degraded.networkFailure("heartbeat", target);
            } else {
                handle(reply, target);
            }
        });
    }

    private void handle(HttpReply reply, Endpoint target) {
        degraded.answered();
        LenientJson.object(reply.text()).ifPresent(context.replies()::warnings);
        switch (StatusClass.of(reply.status())) {
            case AUTH -> {
                context.console().authFailure("heartbeat HTTP " + reply.status());
                degraded.requestReconnect();
            }
            case NO_SUCH_ENDPOINT -> degraded.notFound("heartbeat HTTP 404", target);
            case BACKEND_FAULT -> degraded.backendFault("heartbeat HTTP " + reply.status(), target);
            default -> {
            }
        }
    }
}
