// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class BukkitRoster implements ServerRoster {

    private final Optional<Server> server;

    BukkitRoster(Optional<Server> server) {
        this.server = server;
    }

    @Override
    public Collection<? extends Player> online() {
        return server.<Collection<? extends Player>>map(Server::getOnlinePlayers).orElse(List.of());
    }

    @Override
    public Optional<Player> player(UUID id) {
        return server.map(running -> running.getPlayer(id));
    }

    @Override
    public World worldOrNull(UUID id) {
        return server.isPresent() ? server.get().getWorld(id) : null;
    }
}
