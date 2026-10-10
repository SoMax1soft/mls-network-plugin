// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.backend.BackendVerdict;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.LatencyResult;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.TodayStats;
import italy.mlsac.net.backend.VerdictListener;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.backend.session.ApiState;
import italy.mlsac.net.backend.session.BackendStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class FakeBackend implements BackendGateway {

    public record Pending(WindowSubmission request, VerdictListener listener, CompletableFuture<PredictOutcome> outcome) {

        public void answer(BackendVerdict... verdicts) {
            for (BackendVerdict verdict : verdicts) {
                listener.onVerdict(verdict);
            }
            outcome.complete(new PredictOutcome.Completed(verdicts.length));
        }
    }

    private final List<Pending> predictions = new ArrayList<>();
    private final List<AlertReport> alerts = new ArrayList<>();
    private final List<PunishReport> punishments = new ArrayList<>();
    private boolean usable = true;

    public synchronized void usable(boolean value) {
        usable = value;
    }

    @Override
    public synchronized BackendStatus status() {
        return new BackendStatus(usable ? ApiState.CONNECTED : ApiState.DISCONNECTED, Optional.of("http://fake"),
                Endpoint.PRIMARY, usable, false);
    }

    @Override
    public synchronized boolean usable() {
        return usable;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public synchronized CompletableFuture<PredictOutcome> predict(WindowSubmission request, VerdictListener listener) {
        CompletableFuture<PredictOutcome> outcome = new CompletableFuture<>();
        predictions.add(new Pending(request, listener, outcome));
        return outcome;
    }

    @Override
    public synchronized void sendAlertNotice(AlertReport report) {
        alerts.add(report);
    }

    @Override
    public synchronized void sendPunishNotice(PunishReport report) {
        punishments.add(report);
    }

    @Override
    public CompletableFuture<LatencyResult> probeLatency() {
        return CompletableFuture.completedFuture(LatencyResult.of(List.of(12L, 14L, 0L, 10L, 12L), 5));
    }

    @Override
    public CompletableFuture<Optional<TodayStats>> todayStats() {
        return CompletableFuture.completedFuture(Optional.of(new TodayStats("2026-09-30", 10, 1000, 990, 3)));
    }

    public synchronized List<Pending> predictions() {
        return List.copyOf(predictions);
    }

    public synchronized List<AlertReport> alerts() {
        return List.copyOf(alerts);
    }

    public synchronized List<PunishReport> punishments() {
        return List.copyOf(punishments);
    }
}
