// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PunishCooldown {

    static final long WINDOW_MILLIS = 5_000L;

    private final Map<UUID, Long> lastPunishingRung = new ConcurrentHashMap<>();

    public boolean blocks(UUID player, long clockMillis) {
        Long last = lastPunishingRung.get(player);
        return last != null && clockMillis - last < WINDOW_MILLIS;
    }

    public void record(UUID player, long clockMillis) {
        lastPunishingRung.put(player, clockMillis);
    }

    public void forget(UUID player) {
        lastPunishingRung.remove(player);
    }

    public void clear() {
        lastPunishingRung.clear();
    }
}
