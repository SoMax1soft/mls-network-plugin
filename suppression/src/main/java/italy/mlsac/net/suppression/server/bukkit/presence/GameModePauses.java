// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.presence;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameModePauses {

    private final Set<UUID> paused = ConcurrentHashMap.newKeySet();

    public void pause(UUID suspectId) {
        paused.add(suspectId);
    }

    public void resume(UUID suspectId) {
        paused.remove(suspectId);
    }

    public boolean paused(UUID suspectId) {
        return paused.contains(suspectId);
    }

    public void clear() {
        paused.clear();
    }
}
