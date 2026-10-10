// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.plan.TrickType;
import java.util.EnumSet;
import java.util.Set;

public final class OncePerTrigger implements TrickRepeat {

    private final Set<TrickType> seen = EnumSet.noneOf(TrickType.class);

    @Override
    public boolean admit(TrickType type) {
        return seen.add(type);
    }
}
