// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import italy.mlsac.net.pipeline.player.PlayerSample;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CountedStates implements PlayerStates {

    private final PlayerStates server;
    private volatile int lastRosterSize;

    public CountedStates(PlayerStates server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public List<UUID> connectedPlayers() {
        List<UUID> connected = server.connectedPlayers();
        lastRosterSize = connected.size();
        return connected;
    }

    @Override
    public PlayerSample sample(UUID player) {
        return server.sample(player);
    }

    @Override
    public int serverPing(UUID player) {
        return server.serverPing(player);
    }

    public int lastRosterSize() {
        return lastRosterSize;
    }
}
