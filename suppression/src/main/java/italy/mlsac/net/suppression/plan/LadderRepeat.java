// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public enum LadderRepeat {
    REPEAT_LAST,
    STOP_AFTER_LAST;

    public static LadderRepeat fromSwitch(boolean repeatLast) {
        return repeatLast ? REPEAT_LAST : STOP_AFTER_LAST;
    }
}
