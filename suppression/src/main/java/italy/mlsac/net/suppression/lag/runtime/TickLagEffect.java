// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.lag.LagKind;

public interface TickLagEffect {

    LagKind kind();

    void advanceTick(TickScene scene);

    void onInactive(LagRuntime<?> runtime);
}
