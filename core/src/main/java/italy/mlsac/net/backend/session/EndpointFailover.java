// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;

final class EndpointFailover {

    static final long PRIMARY_PROBE_INTERVAL_MILLIS = 5 * 60 * 1000L;

    private final BackendContext context;
    private final ConnectionState state;
    private final SessionOpener opener;

    EndpointFailover(BackendContext context, ConnectionState state, SessionOpener opener) {
        this.context = context;
        this.state = state;
        this.opener = opener;
    }

    boolean eligible(Endpoint target) {
        return target == Endpoint.PRIMARY && context.address().reserveDistinct() && !state.stopping()
                && state.current() == Endpoint.PRIMARY;
    }

    void start(FailoverTrigger trigger, String reason, Runnable onReserveFailed) {
        if (!state.beginFailover()) {
            return;
        }
        String primary = context.address().primary();
        String reserve = context.address().base(Endpoint.RESERVE);
        opener.openOn(Endpoint.RESERVE).whenComplete((outcome, error) -> {
            try {
                if (state.stopping()) {
                    return;
                }
                if (outcome instanceof OpenOutcome.Opened opened) {
                    state.switched(Endpoint.RESERVE, opened.sessionKey(), opened.wire());
                    state.notePrimaryLeft(context.clock().wallMillis("http.failover"));
                    context.console().switchedToReserve(reserve, primary, reason);
                } else {
                    String why = outcome instanceof OpenOutcome.Rejected rejected ? rejected.reason()
                            : String.valueOf(error);
                    context.console().reserveFailed(reserve, why);
                    if (trigger.entersErrorStateWhenReserveFails()) {
                        onReserveFailed.run();
                    }
                }
            } finally {
                state.endFailover();
            }
        });
    }

    void probePrimaryIfDue() {
        long now = context.clock().wallMillis("http.primary_probe");
        if (!state.primaryProbeDue(now, PRIMARY_PROBE_INTERVAL_MILLIS)) {
            return;
        }
        byte[] body = context.bodies().status(context.identity(), state.sessionKey(), context.connectedPlayers().getAsInt());
        context.post(Endpoint.PRIMARY, "heartbeat", true, body).whenComplete((reply, error) -> {
            if (error == null && reply.success() && !state.stopping()) {
                reopenPrimary();
            }
        });
    }

    private void reopenPrimary() {
        if (!state.beginFailover()) {
            return;
        }
        opener.openOn(Endpoint.PRIMARY).whenComplete((outcome, error) -> {
            try {
                if (outcome instanceof OpenOutcome.Opened opened && !state.stopping()) {
                    state.switched(Endpoint.PRIMARY, opened.sessionKey(), opened.wire());
                    context.console().backToPrimary(context.address().primary());
                }
            } finally {
                state.endFailover();
            }
        });
    }
}
