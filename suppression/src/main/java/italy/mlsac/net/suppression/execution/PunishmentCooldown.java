// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.execution;

import italy.mlsac.net.suppression.time.WallClock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PunishmentCooldown {

    public static final long COOLDOWN_MILLIS = 5000L;

    private final WallClock clock;
    private final Map<UUID, Long> lastPunishingStage = new ConcurrentHashMap<>();

    public PunishmentCooldown(WallClock clock) {
        this.clock = clock;
    }

    public boolean blocks(UUID suspectId) {
        Long last = lastPunishingStage.get(suspectId);
        return last != null && clock.epochMillis() - last < COOLDOWN_MILLIS;
    }

    public void record(UUID suspectId) {
        lastPunishingStage.put(suspectId, clock.epochMillis());
    }

    public void forget(UUID suspectId) {
        lastPunishingStage.remove(suspectId);
    }

    public void clear() {
        lastPunishingStage.clear();
    }
}
