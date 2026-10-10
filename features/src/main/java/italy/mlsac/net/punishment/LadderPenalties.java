// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.FlagPenalties;
import java.util.function.Supplier;

public final class LadderPenalties implements FlagPenalties {

    private final Supplier<PenaltyRules> rules;
    private final RungRunner rungs;

    public LadderPenalties(Supplier<PenaltyRules> rules, RungRunner rungs) {
        this.rules = rules;
        this.rungs = rungs;
    }

    @Override
    public void onFlag(FlagContext flag) {
        rules.get().ladder().rungFor(flag.offenceLevel()).ifPresent(rung -> rungs.run(flag, rung));
    }
}
