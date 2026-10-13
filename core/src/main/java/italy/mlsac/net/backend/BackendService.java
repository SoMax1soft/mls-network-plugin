// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.backend.request.SuppressionReport;
import italy.mlsac.net.backend.session.BackendStatus;
import italy.mlsac.net.backend.session.RunningSession;
import italy.mlsac.net.backend.session.SessionSource;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BackendService implements BackendGateway {

    private final SessionSource source;
    private final PluginLog log;
    private volatile RunningSession running;

    public BackendService(SessionSource source, PluginLog log) {
        this.source = source;
        this.log = log;
    }

    public synchronized void start(CoreSettings core) {
        if (running != null) {
            return;
        }
        if (!core.accessKey().configured()) {
            log.warn(LogTopic.BACKEND, "API key is not configured - set detection.api-key in config.yml");
            return;
        }
        RunningSession opened = source.open(core);
        running = opened;
        opened.session().start();
    }

    public synchronized CompletableFuture<Void> stop() {
        RunningSession current = running;
        running = null;
        if (current == null) {
            return CompletableFuture.completedFuture(null);
        }
        return current.session().disconnect().whenComplete((ignored, error) -> close(current));
    }

    @Override
    public boolean exists() {
        return running != null;
    }

    @Override
    public BackendStatus status() {
        RunningSession current = running;
        return current == null ? BackendStatus.notInitialized() : current.session().status();
    }

    @Override
    public boolean usable() {
        RunningSession current = running;
        return current != null && current.session().usable();
    }

    @Override
    public CompletableFuture<PredictOutcome> predict(WindowSubmission request, VerdictListener listener) {
        RunningSession current = running;
        if (current == null) {
            return CompletableFuture.completedFuture(
                    new PredictOutcome.Refused(PredictOutcome.RefusalReason.NOT_INITIALIZED));
        }
        return current.session().predict(request, listener);
    }

    @Override
    public void sendAlertNotice(AlertReport report) {
        RunningSession current = running;
        if (current != null) {
            current.session().sendAlertNotice(report);
        }
    }

    @Override
    public void sendPunishNotice(PunishReport report) {
        RunningSession current = running;
        if (current != null) {
            current.session().sendPunishNotice(report);
        }
    }

    @Override
    public void sendSuppressionNotice(SuppressionReport report) {
        RunningSession current = running;
        if (current != null) {
            current.session().sendSuppressionNotice(report);
        }
    }

    @Override
    public CompletableFuture<LatencyResult> probeLatency() {
        RunningSession current = running;
        return current == null ? CompletableFuture.completedFuture(LatencyResult.of(List.of(), 0))
                : current.session().probeLatency();
    }

    @Override
    public CompletableFuture<Optional<TodayStats>> todayStats() {
        RunningSession current = running;
        return current == null ? CompletableFuture.completedFuture(Optional.empty()) : current.session().todayStats();
    }

    @Override
    public CompletableFuture<DossierReply> dossier(DossierQuery query) {
        RunningSession current = running;
        return current == null ? CompletableFuture.completedFuture(new DossierReply.Unavailable())
                : current.session().dossier(query);
    }

    private void close(RunningSession session) {
        try {
            session.resources().close();
        } catch (Exception failure) {
            log.warn(LogTopic.BACKEND, "Backend connection did not close cleanly: " + failure.getMessage());
        }
    }
}
