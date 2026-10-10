// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.plan.TrickType;

public interface TrickRepeat {

    boolean admit(TrickType type);
}
