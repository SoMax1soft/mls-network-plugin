// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import java.util.Set;
import org.bukkit.entity.Player;

final class ClimbableBlock implements ClimbCheck {

    private static final Set<String> CLIMBABLE = Set.of("LADDER", "VINE", "SCAFFOLDING", "TWISTING_VINES",
            "TWISTING_VINES_PLANT", "WEEPING_VINES", "WEEPING_VINES_PLANT");

    @Override
    public boolean climbing(Player player) {
        return CLIMBABLE.contains(player.getLocation().getBlock().getType().name());
    }
}
