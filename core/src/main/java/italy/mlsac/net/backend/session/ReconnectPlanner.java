// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;

final class ReconnectPlanner {

    private final BackendContext context;
    private final ConnectionState state;
    private final ReconnectBackoff backoff = new ReconnectBackoff();
    private volatile Runnable reconnect = () -> { };
    private volatile boolean enabled = true;
    private volatile TaskTicket pending;

    ReconnectPlanner(BackendContext context, ConnectionState state) {
        this.context = context;
        this.state = state;
    }

    void bind(Runnable reconnectAction) {
        this.reconnect = reconnectAction;
    }

    void request() {
        if (!enabled || state.stopping()) {
            return;
        }
        if (!state.claimReconnectSlot()) {
            context.console().debug("reconnect already scheduled");
            return;
        }
        int attempt = state.takeReconnectAttempt();
        long base = backoff.baseMillis(attempt);
        long jitter = context.random().nextLong("http.reconnect_jitter", backoff.jitterBound(base));
        long delay = base + jitter;
        context.console().reconnectScheduled(delay / 1000, attempt + 1);
        pending = context.scheduler().submit(TaskLane.BACKGROUND, TaskTiming.after(backoff.delayTicks(delay)), () -> {
            state.releaseReconnectSlot();
            reconnect.run();
        });
    }

    void disable() {
        enabled = false;
        TaskTicket task = pending;
        if (task != null) {
            task.cancel();
        }
    }
}
