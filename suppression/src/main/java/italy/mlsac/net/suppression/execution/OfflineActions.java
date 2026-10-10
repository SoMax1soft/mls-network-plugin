// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.plan.InGameActionSink;
import italy.mlsac.net.suppression.plan.TimedAction;
import italy.mlsac.net.suppression.plan.TrickAction;
import italy.mlsac.net.suppression.suspect.SuspectState;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.Optional;

final class OfflineActions implements InGameActionSink {

    private final ActionScene scene;
    private final TriggerContext context;
    private final long now;
    private final ActionReport report = new ActionReport();

    OfflineActions(ActionScene scene, TriggerContext context, long now) {
        this.scene = scene;
        this.context = context;
        this.now = now;
    }

    ActionReport report() {
        return report;
    }

    @Override
    public void timed(TimedAction action) {
        Optional<SuspectState> state = scene.registry().retainedOffline(context.suspectId(), now);
        if (state.isEmpty()) {
            return;
        }
        EffectSnapshot snapshot = state.get().record(action, now);
        scene.watcher().effectsChanged(context.suspectId(), snapshot);
        report.add(action.token());
    }

    @Override
    public void trick(TrickAction action) {
    }
}
