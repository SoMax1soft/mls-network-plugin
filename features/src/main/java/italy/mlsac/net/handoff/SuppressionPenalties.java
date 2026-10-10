// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.handoff;

import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.FlagPenalties;

public final class SuppressionPenalties implements FlagPenalties {

    private final SuppressionEngine engine;

    public SuppressionPenalties(SuppressionEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onFlag(FlagContext flag) {
        engine.flagged(flag);
    }
}
