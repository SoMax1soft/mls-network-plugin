// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.entity.Player;

final class ClimbableUnderfoot implements PlayerTrait {

    @Override
    public boolean of(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return Tag.CLIMBABLE.isTagged(player.getLocation().getBlock().getType());
    }
}
