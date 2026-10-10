// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.List;

public record SuppressionStep(int flag, List<SuppressionAction> actions) {

    public SuppressionStep {
        actions = List.copyOf(actions);
    }
}
