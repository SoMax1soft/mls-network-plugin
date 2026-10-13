// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.LatencyResult;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.TodayStats;
import italy.mlsac.net.backend.VerdictListener;
import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.backend.request.SuppressionReport;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BackendSession {

    private static final long[] STARTUP_WAIT_TICKS = {20, 40, 80};
    private static final int STARTUP_ATTEMPTS = 3;

    private final BackendContext context;
    private final ConnectionState state = new ConnectionState();
    private final KeepAliveJobs jobs;
    private final ReconnectPlanner planner;
    private final SessionOpener opener;
    private final DegradedModes degraded;
    private final PredictChannel predicts;
    private final EventChannel events;
    private final OperatorQueries queries;
    private final DossierQueries dossiers;

    public BackendSession(BackendContext context) {
        this.context = context;
        this.jobs = new KeepAliveJobs(context.scheduler());
        this.planner = new ReconnectPlanner(context, state);
        this.opener = new SessionOpener(context);
        EndpointFailover failover = new EndpointFailover(context, state, opener);
        this.degraded = new DegradedModes(context, state, jobs, planner, failover);
        this.predicts = new PredictChannel(context, state, degraded);
        this.events = new EventChannel(context, state, degraded);
        this.queries = new OperatorQueries(context, state);
        this.dossiers = new DossierQueries(context, state);
        HeartbeatExchange heartbeat = new HeartbeatExchange(context, state, degraded, failover);
        OnlineExchange online = new OnlineExchange(context, state, degraded);
        PollExchange poll = new PollExchange(context, state, degraded);
        jobs.bind(KeepAliveJobs.Job.HEARTBEAT, heartbeat::run);
        jobs.bind(KeepAliveJobs.Job.ONLINE, online::run);
        jobs.bind(KeepAliveJobs.Job.POLL, poll::run);
        jobs.bind(KeepAliveJobs.Job.CHECK, this::healthProbe);
        jobs.bind(KeepAliveJobs.Job.STASIS_PROBE, this::stasisProbe);
        planner.bind(this::reconnectNow);
    }

    public void start() {
        context.console().connecting(context.address().primary());
        if (context.address().plaintextRemote()) {
            context.console().plaintextEndpoint(context.address().primary());
        }
        startupAttempt(1);
    }

    public BackendStatus status() {
        return state.snapshot(context.address());
    }

    public boolean usable() {
        return state.connected() && !state.stasis() && !degraded.serverErrorActive();
    }

    public CompletableFuture<PredictOutcome> predict(WindowSubmission request, VerdictListener listener) {
        return predicts.predict(request, listener);
    }

    public void sendAlertNotice(AlertReport report) {
        events.alert(report);
    }

    public void sendPunishNotice(PunishReport report) {
        events.punish(report);
    }

    public void sendSuppressionNotice(SuppressionReport report) {
        events.suppression(report);
    }

    public CompletableFuture<LatencyResult> probeLatency() {
        return queries.probeLatency();
    }

    public CompletableFuture<Optional<TodayStats>> todayStats() {
        return queries.todayStats();
    }

    public CompletableFuture<DossierReply> dossier(DossierQuery query) {
        return dossiers.ask(query);
    }

    public CompletableFuture<Void> disconnect() {
        state.shutdown();
        planner.disable();
        jobs.close();
        return CompletableFuture.runAsync(() -> {
            context.console().debug("disconnected");
            context.transport().cancelInFlight();
        });
    }

    private void startupAttempt(int attempt) {
        opener.attempt().whenComplete((outcome, error) -> {
            if (state.stopping()) {
                return;
            }
            if (outcome instanceof OpenOutcome.Opened opened) {
                onOpened(opened, false);
                return;
            }
            String reason = outcome instanceof OpenOutcome.Rejected rejected ? rejected.reason() : String.valueOf(error);
            long wait = STARTUP_WAIT_TICKS[attempt - 1];
            if (attempt < STARTUP_ATTEMPTS) {
                context.console().debug("startup try " + attempt + " of " + STARTUP_ATTEMPTS + " failed, next one after " + wait * 50 + " ms");
                context.scheduler().submit(TaskLane.BACKGROUND, TaskTiming.after(wait), () -> startupAttempt(attempt + 1));
            } else {
                context.scheduler().submit(TaskLane.BACKGROUND, TaskTiming.after(wait), () -> giveUpForeground(reason));
            }
        });
    }

    private void giveUpForeground(String reason) {
        if (state.stopping()) {
            return;
        }
        context.console().startFailed(context.address().primary(), reason);
        jobs.start(KeepAliveJobs.Job.CHECK);
        planner.request();
    }

    private void onOpened(OpenOutcome.Opened opened, boolean reconnect) {
        state.opened(opened.endpoint(), opened.sessionKey(), opened.wire());
        context.console().debug("Ultra windows use wire version " + opened.wire().version());
        context.console().connected(context.address().base(opened.endpoint()),
                opened.endpoint() == Endpoint.RESERVE, reconnect);
        jobs.start(KeepAliveJobs.Job.HEARTBEAT, KeepAliveJobs.Job.ONLINE, KeepAliveJobs.Job.POLL,
                KeepAliveJobs.Job.CHECK);
    }

    private void reconnectNow() {
        if (state.stopping() || state.connected()) {
            return;
        }
        opener.attempt().whenComplete((outcome, error) -> {
            if (state.stopping()) {
                return;
            }
            if (outcome instanceof OpenOutcome.Opened opened) {
                onOpened(opened, true);
            } else {
                planner.request();
            }
        });
    }

    private void healthProbe() {
        if (state.stopping()) {
            return;
        }
        if (degraded.serverErrorActive()) {
            planner.request();
        } else if (!state.quotaExceeded() && !state.connected()) {
            planner.request();
        }
    }

    private void stasisProbe() {
        if (state.stopping() || !state.stasis()) {
            return;
        }
        byte[] body = context.bodies().status(context.identity(), state.sessionKey(), context.connectedPlayers().getAsInt());
        context.post(state.current(), "heartbeat", true, body).whenComplete((reply, error) -> {
            if (state.stopping()) {
                return;
            }
            if (error == null && reply.success()) {
                degraded.leaveStasis();
            } else {
                context.console().debug("stasis probe: " + (error != null ? "no answer" : "HTTP " + reply.status()));
            }
        });
    }
}
