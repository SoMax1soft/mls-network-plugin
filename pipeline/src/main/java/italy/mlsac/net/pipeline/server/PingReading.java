// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PingReading {

    int ABSENT = -1;

    PingReading NONE = player -> ABSENT;

    int millis(Player player);

    static PingReading forRunningServer() {
        if (MethodPresence.declares(Player.class, "getPing")) {
            return new PlayerPing();
        }
        if (MethodPresence.declares(Player.Spigot.class, "getPing")) {
            return new SpigotPing();
        }
        return NONE;
    }
}
