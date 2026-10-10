// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import org.bukkit.entity.Player;

final class ClimbingFlag implements ClimbCheck {

    @Override
    public boolean climbing(Player player) {
        return player.isClimbing();
    }
}
