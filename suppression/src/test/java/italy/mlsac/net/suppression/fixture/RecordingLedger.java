// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.server.SuppressionLedger;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.ArrayList;
import java.util.List;

public final class RecordingLedger implements SuppressionLedger {

    public record Entry(String trigger, int level, String player, double buffer, List<String> actions) {
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void bufferTriggered(TriggerContext context, int percent, List<String> actions) {
        entries.add(new Entry("buffer", percent, context.suspectName(), context.buffer(), List.copyOf(actions)));
    }

    @Override
    public void stageRan(TriggerContext context, int stage, List<String> actions) {
        entries.add(new Entry("stage", stage, context.suspectName(), context.buffer(), List.copyOf(actions)));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
