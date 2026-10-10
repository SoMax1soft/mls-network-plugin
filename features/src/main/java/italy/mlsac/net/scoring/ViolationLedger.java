// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ViolationLedger {

    private final Map<UUID, Integer> levels = new ConcurrentHashMap<>();

    public int raise(UUID player) {
        return levels.merge(player, 1, Integer::sum);
    }

    public int level(UUID player) {
        return levels.getOrDefault(player, 0);
    }

    public void decay(int amount, Predicate<UUID> engagedInFight) {
        for (UUID player : levels.keySet()) {
            if (engagedInFight.test(player)) {
                continue;
            }
            levels.computeIfPresent(player, (id, level) -> level - amount <= 0 ? null : level - amount);
        }
    }

    public void forget(UUID player) {
        levels.remove(player);
    }

    public void clear() {
        levels.clear();
    }
}
