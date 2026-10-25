// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.turn.PlayerTurn;
import java.util.List;
import java.util.UUID;

public interface PlayerStates {
    List<UUID> connectedPlayers();

    PlayerSample sample(UUID player);

    default PlayerSample sample(UUID player, boolean complete) {
        return sample(player);
    }

    default PlayerSample sample(UUID player, boolean complete, PlayerTurn turn) {
        return sample(player, complete);
    }

    int serverPing(UUID player);

    default int serverPingAfterSample(UUID player) {
        return serverPing(player);
    }
}
