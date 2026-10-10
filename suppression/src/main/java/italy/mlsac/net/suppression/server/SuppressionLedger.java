// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.List;

public interface SuppressionLedger {

    SuppressionLedger NONE = new SuppressionLedger() {
        @Override
        public void bufferTriggered(TriggerContext context, int percent, List<String> actions) {
        }

        @Override
        public void stageRan(TriggerContext context, int stage, List<String> actions) {
        }
    };

    void bufferTriggered(TriggerContext context, int percent, List<String> actions);

    void stageRan(TriggerContext context, int stage, List<String> actions);
}
