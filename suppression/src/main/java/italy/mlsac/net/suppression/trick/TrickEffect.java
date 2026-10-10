// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.server.SuspectBody;

public interface TrickEffect {

    TrickType type();

    boolean perform(SuspectBody body, TrickTuning tuning);
}
