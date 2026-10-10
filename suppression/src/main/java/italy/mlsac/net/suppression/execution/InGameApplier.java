// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.plan.InGameAction;
import italy.mlsac.net.suppression.time.MonotonicClock;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.List;

public final class InGameApplier {

    private final ActionScene scene;
    private final CurrentPlan plan;
    private final MonotonicClock clock;

    public InGameApplier(ActionScene scene, CurrentPlan plan, MonotonicClock clock) {
        this.scene = scene;
        this.plan = plan;
        this.clock = clock;
    }

    public ActionReport applyOnline(List<InGameAction> actions, TriggerContext context, TrickRepeat repeat) {
        ActionReport report = new ActionReport();
        OnlineActions online = new OnlineActions(
                new OnlineActions.Round(scene, plan.get(), context, repeat, clock.clockMillis()), report);
        actions.forEach(action -> action.offerTo(online));
        return report;
    }

    public ActionReport applyOffline(List<InGameAction> actions, TriggerContext context) {
        OfflineActions offline = new OfflineActions(scene, context, clock.clockMillis());
        actions.forEach(action -> action.offerTo(offline));
        return offline.report();
    }
}
