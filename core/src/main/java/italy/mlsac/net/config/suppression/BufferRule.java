// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.List;

public record BufferRule(int percent, boolean enabled, int restSeconds, List<SuppressionAction> actions) {

    public BufferRule {
        actions = List.copyOf(actions);
    }

    public double threshold(double capacity) {
        return percent * capacity / 100.0;
    }
}
