// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

final class SpigotPing implements PingReading {

    @Override
    @SuppressWarnings("deprecation")
    public int millis(Player player) {
        return player.spigot().getPing();
    }
}
