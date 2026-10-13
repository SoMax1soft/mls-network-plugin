// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.scoring.FlagContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class MaximumPunishment {

    static final double FORCED_PROBABILITY = 1.0;
    static final double FORCED_BUFFER = 100.0;

    private final Supplier<PenaltyRules> rules;
    private final RungRunner rungs;

    public MaximumPunishment(Supplier<PenaltyRules> rules, RungRunner rungs) {
        this.rules = rules;
        this.rungs = rungs;
    }

    public Optional<Integer> punish(UUID player, String playerNick) {
        Optional<Map.Entry<Integer, List<String>>> top = rules.get().ladder().top();
        top.ifPresent(rung -> rungs.runWithoutCooldown(
                new FlagContext(player, playerNick, rung.getKey(), FORCED_PROBABILITY, FORCED_BUFFER), rung.getValue(),
                CommandOrigin.punish(rung.getKey())));
        return top.map(Map.Entry::getKey);
    }
}
