// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.plan.TrickType;

public final class EveryTrick implements TrickRepeat {

    @Override
    public boolean admit(TrickType type) {
        return true;
    }
}
