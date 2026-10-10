// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

enum CloseRangeColumn {
    STEP_CHANGE,
    CURVATURE,
    STEP_SIZE,
    TURN,
    LAG,
    TURN_TIMES_LAG,
    TURN_SQUARED,
    LAG_SQUARED,
    PULL,
    PULL_TIMES_LAG,
    PULL_SQUARED,
    SETTLED_CHANGE,
    SETTLED_SIZE;

    static final int COUNT = values().length;

    int at() {
        return ordinal();
    }
}
