// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import italy.mlsac.net.pipeline.player.PlayerSample;
import java.util.List;
import java.util.UUID;

public interface PlayerStates {
    List<UUID> connectedPlayers();

    PlayerSample sample(UUID player);

    int serverPing(UUID player);
}
