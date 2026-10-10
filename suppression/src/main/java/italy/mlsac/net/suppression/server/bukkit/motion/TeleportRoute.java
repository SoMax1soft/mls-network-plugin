// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportRoute {

    void travel(Player player, Location target, Runnable arrived);
}
