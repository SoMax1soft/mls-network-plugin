// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.json.JsonObject;
import java.util.Optional;

final class PollExchange {

    private final BackendContext context;
    private final ConnectionState state;
    private final DegradedModes degraded;

    PollExchange(BackendContext context, ConnectionState state, DegradedModes degraded) {
        this.context = context;
        this.state = state;
        this.degraded = degraded;
    }

    void run() {
        if (state.stopping() || !state.connected() || state.sessionKey().isEmpty() || degraded.serverErrorActive()
                || !state.claimPoll()) {
            return;
        }
        Endpoint target = state.current();
        byte[] body = context.bodies().poll(context.identity(), state.sessionKey());
        context.post(target, "events/poll", true, body).whenComplete((reply, error) -> {
            try {
                if (state.stopping()) {
                    return;
                }
                if (error != null) {
                    degraded.networkFailure("interserver-poll", target);
                } else {
                    handle(reply, target);
                }
            } finally {
                state.releasePoll();
            }
        });
    }

    private void handle(HttpReply reply, Endpoint target) {
        degraded.answered();
        Optional<JsonObject> json = LenientJson.object(reply.text());
        json.ifPresent(context.replies()::warnings);
        switch (StatusClass.of(reply.status())) {
            case SUCCESS -> json.ifPresent(context.replies()::interServerEvents);
            case NO_SUCH_ENDPOINT -> degraded.notFound("events/poll HTTP 404", target);
            case METHOD_NOT_ALLOWED -> {
            }
            case AUTH -> degraded.markDisconnected();
            default -> context.console().debug("events/poll HTTP " + reply.status());
        }
    }
}
