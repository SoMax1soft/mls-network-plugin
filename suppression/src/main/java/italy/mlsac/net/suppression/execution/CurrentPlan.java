// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.plan.SuppressionPlan;
import java.util.concurrent.atomic.AtomicReference;

public final class CurrentPlan {

    private final AtomicReference<SuppressionPlan> plan;

    public CurrentPlan(SuppressionPlan initial) {
        this.plan = new AtomicReference<>(initial);
    }

    public SuppressionPlan get() {
        return plan.get();
    }

    public void replace(SuppressionPlan next) {
        plan.set(next);
    }
}
