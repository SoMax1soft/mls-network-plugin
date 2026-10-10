// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitPlayerDirectory implements PlayerDirectory {

    @Override
    public Optional<PlayerView> byId(UUID id) {
        Player player = Bukkit.getPlayer(id);
        return player == null ? Optional.empty() : Optional.of(new BukkitPlayerView(player));
    }

    @Override
    @SuppressWarnings("deprecation")
    public Optional<PlayerView> byNamePrefix(String name) {
        Player player = Bukkit.getPlayer(name);
        return player == null ? Optional.empty() : Optional.of(new BukkitPlayerView(player));
    }

    @Override
    public Optional<PlayerView> byExactName(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null ? Optional.empty() : Optional.of(new BukkitPlayerView(player));
    }

    @Override
    public List<PlayerView> online() {
        List<PlayerView> views = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            views.add(new BukkitPlayerView(player));
        }
        return views;
    }
}
