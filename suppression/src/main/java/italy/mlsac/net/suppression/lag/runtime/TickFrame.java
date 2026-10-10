// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.motion.WorldPoint;
import java.util.EnumSet;
import java.util.Set;

public record TickFrame(long now, long tick, WorldPoint position, boolean grounded, double pitch,
                        float fallBlocks, Set<BodyCondition> conditions) {

    public TickFrame {
        conditions = conditions.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(conditions));
    }

    public boolean holdsAnyOf(Set<BodyCondition> wanted) {
        for (BodyCondition condition : wanted) {
            if (conditions.contains(condition)) {
                return true;
            }
        }
        return false;
    }
}
