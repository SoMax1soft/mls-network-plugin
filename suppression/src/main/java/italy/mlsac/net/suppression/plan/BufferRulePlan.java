// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.List;

public record BufferRulePlan(int percent, boolean enabled, int restSeconds, List<InGameAction> actions) {

    public BufferRulePlan {
        actions = List.copyOf(actions);
    }

    public long quietPeriodMillis() {
        return restSeconds * 1000L;
    }
}
