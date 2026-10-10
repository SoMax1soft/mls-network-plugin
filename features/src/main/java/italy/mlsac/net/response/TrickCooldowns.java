// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrickCooldowns {

    private final Map<Key, Long> readyAt = new ConcurrentHashMap<>();

    public boolean ready(UUID player, String type, long clockMillis) {
        Long until = readyAt.get(new Key(player, type));
        return until == null || clockMillis >= until;
    }

    public void start(UUID player, String type, int restSeconds, long clockMillis) {
        if (restSeconds > 0) {
            readyAt.put(new Key(player, type), clockMillis + restSeconds * 1000L);
        }
    }

    public void forget(UUID player) {
        readyAt.keySet().removeIf(key -> key.player().equals(player));
    }

    public void clear() {
        readyAt.clear();
    }

    private record Key(UUID player, String type) {
    }
}
