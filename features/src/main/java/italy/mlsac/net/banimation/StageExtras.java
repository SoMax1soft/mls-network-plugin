// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

public enum StageExtras {
    IGNORED_AS_BEFORE,
    APPLIED;

    boolean reads() {
        return this == APPLIED;
    }
}
