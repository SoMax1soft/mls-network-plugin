// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class FixedRoster implements ServerRoster {

    private final List<Player> players = new ArrayList<>();
    private final Map<UUID, World> worlds = new LinkedHashMap<>();

    FixedRoster with(Player player) {
        players.add(player);
        return this;
    }

    FixedRoster with(UUID id, World world) {
        worlds.put(id, world);
        return this;
    }

    void remove(Player player) {
        players.remove(player);
    }

    @Override
    public Collection<? extends Player> online() {
        return List.copyOf(players);
    }

    @Override
    public Optional<Player> player(UUID id) {
        return players.stream().filter(player -> player.getUniqueId().equals(id)).findFirst();
    }

    @Override
    public World worldOrNull(UUID id) {
        return worlds.get(id);
    }
}
