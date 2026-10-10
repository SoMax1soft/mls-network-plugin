// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface ViewTurn {

    void turn(Player player, int syncId, double yawDelta, double pitchDelta);
}
