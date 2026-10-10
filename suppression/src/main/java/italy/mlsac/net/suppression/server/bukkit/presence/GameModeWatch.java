// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.presence;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class GameModeWatch implements Listener {

    private final GameModePauses pauses;

    public GameModeWatch(GameModePauses pauses) {
        this.pauses = pauses;
    }

    public void seed(Collection<? extends Player> online) {
        for (Player player : online) {
            note(player.getUniqueId(), player.getGameMode());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void joined(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        note(player.getUniqueId(), player.getGameMode());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void modeSwitched(PlayerGameModeChangeEvent event) {
        note(event.getPlayer().getUniqueId(), event.getNewGameMode());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void left(PlayerQuitEvent event) {
        pauses.resume(event.getPlayer().getUniqueId());
    }

    private void note(UUID suspectId, GameMode mode) {
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            pauses.pause(suspectId);
        } else {
            pauses.resume(suspectId);
        }
    }
}
