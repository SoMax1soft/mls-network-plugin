// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.backend.request.SuppressionReport;

final class EventChannel {

    private final BackendContext context;
    private final ConnectionState state;
    private final DegradedModes degraded;

    EventChannel(BackendContext context, ConnectionState state, DegradedModes degraded) {
        this.context = context;
        this.state = state;
        this.degraded = degraded;
    }

    void alert(AlertReport report) {
        if (allowed()) {
            send(context.bodies().alert(context.identity(), state.sessionKey(), report));
        }
    }

    void punish(PunishReport report) {
        if (allowed()) {
            send(context.bodies().punish(context.identity(), state.sessionKey(), report));
        }
    }

    void suppression(SuppressionReport report) {
        if (allowed()) {
            send(context.bodies().suppression(context.identity(), state.sessionKey(), report));
        }
    }

    private boolean allowed() {
        return !state.stopping() && state.connected() && !degraded.serverErrorActive();
    }

    private void send(byte[] body) {
        Endpoint target = state.current();
        context.post(target, "events", true, body).whenComplete((reply, error) -> {
            if (state.stopping()) {
                return;
            }
            if (error != null) {
                degraded.uncountedTransportFailure("events", target);
            } else {
                handle(reply, target);
            }
        });
    }

    private void handle(HttpReply reply, Endpoint target) {
        LenientJson.object(reply.text()).ifPresent(context.replies()::warnings);
        switch (StatusClass.of(reply.status())) {
            case NO_SUCH_ENDPOINT -> degraded.notFound("events HTTP 404", target);
            case AUTH -> degraded.markDisconnected();
            case BACKEND_FAULT -> degraded.backendFault("events HTTP " + reply.status(), target);
            default -> {
            }
        }
    }
}
