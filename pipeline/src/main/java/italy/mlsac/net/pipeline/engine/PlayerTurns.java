// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.turn.PlayerTurn;
import italy.mlsac.net.pipeline.turn.TurnEpoch;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTurns {
    private final ConcurrentHashMap<UUID, PlayerTurn> turns = new ConcurrentHashMap<>();
    private final TurnEpoch epoch = new TurnEpoch();

    public PlayerTurn of(UUID player) {
        PlayerTurn turn = turns.get(player);
        return turn != null ? turn : turns.computeIfAbsent(player, ignored -> new PlayerTurn(epoch));
    }

    public void retire(UUID player) {
        turns.remove(player);
    }

    public int held() {
        return turns.size();
    }

    public TurnEpoch epoch() {
        return epoch;
    }

}
