// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.Endpoint;
import java.util.OptionalLong;

final class DegradedModes {

    static final long SERVER_ERROR_MILLIS = 60_000L;
    static final int LOST_AFTER_FAILURES = 3;

    private final BackendContext context;
    private final ConnectionState state;
    private final KeepAliveJobs jobs;
    private final ReconnectPlanner planner;
    private final EndpointFailover failover;

    DegradedModes(BackendContext context, ConnectionState state, KeepAliveJobs jobs, ReconnectPlanner planner,
                  EndpointFailover failover) {
        this.context = context;
        this.state = state;
        this.jobs = jobs;
        this.planner = planner;
        this.failover = failover;
    }

    void answered() {
        state.recordAnswer();
    }

    void networkFailure(String where, Endpoint target) {
        if (failover.eligible(target)) {
            failover.start(FailoverTrigger.NETWORK, where + " network failure", () -> { });
        }
        int failures = state.noteTransportFault();
        if (failures >= LOST_AFTER_FAILURES && state.markDisconnected()) {
            jobs.cancel(KeepAliveJobs.Job.HEARTBEAT, KeepAliveJobs.Job.ONLINE, KeepAliveJobs.Job.POLL);
            context.console().connectionLost(where);
            planner.request();
        }
    }

    void uncountedTransportFailure(String where, Endpoint target) {
        context.console().debug(where + " transport failure");
        if (failover.eligible(target)) {
            failover.start(FailoverTrigger.NETWORK, where + " network failure", () -> { });
        }
    }

    void notFound(String reason, Endpoint target) {
        if (failover.eligible(target)) {
            failover.start(FailoverTrigger.NO_SUCH_ENDPOINT, reason, this::enterServerErrorWithoutReconnect);
        }
    }

    void backendFault(String reason, Endpoint target) {
        if (failover.eligible(target)) {
            failover.start(FailoverTrigger.BACKEND_FAULT, reason, this::enterServerErrorWithoutReconnect);
            return;
        }
        if (state.stopping()) {
            return;
        }
        if (state.enterServerError(context.clock().wallMillis("http.error_enter"))) {
            context.console().serverErrorEntered(reason);
        }
        planner.request();
    }

    boolean serverErrorActive() {
        OptionalLong since = state.serverErrorSince();
        if (since.isEmpty()) {
            return false;
        }
        long now = context.clock().wallMillis("http.error_expiry");
        if (now - since.getAsLong() > SERVER_ERROR_MILLIS) {
            if (state.clearServerError()) {
                context.console().serverErrorExpired();
            }
            return false;
        }
        return true;
    }

    void clearServerErrorAndQuota() {
        if (state.clearServerError()) {
            context.console().debug("server error state cleared");
        }
        state.quotaExceeded(false);
        leaveStasis();
    }

    void quotaExceeded() {
        state.quotaExceeded(true);
        if (state.enterStasis()) {
            context.console().stasisEntered();
            jobs.cancel(KeepAliveJobs.Job.HEARTBEAT, KeepAliveJobs.Job.ONLINE, KeepAliveJobs.Job.POLL,
                    KeepAliveJobs.Job.CHECK);
            jobs.start(KeepAliveJobs.Job.STASIS_PROBE);
        }
    }

    void leaveStasis() {
        if (state.leaveStasis()) {
            context.console().stasisLeft();
            jobs.cancel(KeepAliveJobs.Job.STASIS_PROBE);
            jobs.start(KeepAliveJobs.Job.HEARTBEAT, KeepAliveJobs.Job.ONLINE, KeepAliveJobs.Job.POLL,
                    KeepAliveJobs.Job.CHECK);
        }
    }

    void sessionUnknown(PredictRoute rejected) {
        if (state.forgetSession(rejected)) {
            jobs.cancel(KeepAliveJobs.Job.HEARTBEAT, KeepAliveJobs.Job.ONLINE, KeepAliveJobs.Job.POLL);
            context.console().debug("the backend no longer knows this session, opening a new one");
            planner.request();
        }
    }

    void markDisconnected() {
        state.markDisconnected();
    }

    void requestReconnect() {
        planner.request();
    }

    private void enterServerErrorWithoutReconnect() {
        if (state.enterServerError(context.clock().wallMillis("http.error_enter"))) {
            context.console().serverErrorEntered("reserve endpoint failed too");
        }
    }
}
