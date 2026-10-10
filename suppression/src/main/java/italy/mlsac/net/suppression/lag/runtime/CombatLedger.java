// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatLedger {

    public static final long COMBAT_WINDOW_MILLIS = 5000L;

    private final Map<UUID, Long> lastCombat = new ConcurrentHashMap<>();

    public void attacked(UUID suspectId, long now) {
        lastCombat.merge(suspectId, now, Math::max);
    }

    public void hurtByPlayer(UUID suspectId, long now) {
        lastCombat.merge(suspectId, now, Math::max);
    }

    public boolean engagedInFight(UUID suspectId, long now) {
        Long last = lastCombat.get(suspectId);
        return last != null && now - last < COMBAT_WINDOW_MILLIS;
    }

    public void forget(UUID suspectId) {
        lastCombat.remove(suspectId);
    }

    public void clear() {
        lastCombat.clear();
    }
}
