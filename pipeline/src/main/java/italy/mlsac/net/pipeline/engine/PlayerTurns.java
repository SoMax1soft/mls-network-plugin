// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTurns {
    private final ConcurrentHashMap<UUID, Object> turns = new ConcurrentHashMap<>();

    public Object of(UUID player) {
        Object turn = turns.get(player);
        return turn != null ? turn : turns.computeIfAbsent(player, PlayerTurns::freshTurn);
    }

    public void retire(UUID player) {
        turns.remove(player);
    }

    public int held() {
        return turns.size();
    }

    private static Object freshTurn(UUID player) {
        return new Object();
    }
}
