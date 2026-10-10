// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.backend.request.SuppressionReport;
import italy.mlsac.net.suppression.server.SuppressionLedger;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.List;

public record BackendSuppressionLedger(BackendGateway backend) implements SuppressionLedger {

    @Override
    public void bufferTriggered(TriggerContext context, int percent, List<String> actions) {
        report(context, SuppressionReport.Trigger.BUFFER, percent, actions);
    }

    @Override
    public void stageRan(TriggerContext context, int stage, List<String> actions) {
        report(context, SuppressionReport.Trigger.STAGE, stage, actions);
    }

    private void report(TriggerContext context, SuppressionReport.Trigger trigger, int level, List<String> actions) {
        backend.sendSuppressionNotice(new SuppressionReport(context.suspectId(), context.suspectName(),
                context.probability(), context.buffer(), trigger, level, actions));
    }
}
