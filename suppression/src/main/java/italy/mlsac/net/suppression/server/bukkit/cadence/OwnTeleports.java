// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OwnTeleports {

    private final Map<UUID, Integer> underway = new ConcurrentHashMap<>();

    public void begin(UUID suspectId) {
        underway.merge(suspectId, 1, Integer::sum);
    }

    public void finish(UUID suspectId) {
        underway.computeIfPresent(suspectId, (id, count) -> count > 1 ? count - 1 : null);
    }

    public boolean underway(UUID suspectId) {
        return underway.containsKey(suspectId);
    }
}
