// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.request.PunishReport;
import italy.mlsac.net.backend.request.SuppressionReport;
import italy.mlsac.net.backend.session.BackendStatus;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BackendGateway {

    BackendStatus status();

    boolean usable();

    boolean exists();

    CompletableFuture<PredictOutcome> predict(WindowSubmission request, VerdictListener listener);

    void sendAlertNotice(AlertReport report);

    void sendPunishNotice(PunishReport report);

    default void sendSuppressionNotice(SuppressionReport report) {
    }

    CompletableFuture<LatencyResult> probeLatency();

    CompletableFuture<Optional<TodayStats>> todayStats();

    default CompletableFuture<DossierReply> dossier(DossierQuery query) {
        return CompletableFuture.completedFuture(new DossierReply.Unavailable());
    }
}
