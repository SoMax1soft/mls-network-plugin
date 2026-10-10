// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ClimbCheck {

    boolean climbing(Player player);

    static ClimbCheck forThisServer() {
        try {
            LivingEntity.class.getMethod("isClimbing");
            return new ClimbingFlag();
        } catch (NoSuchMethodException absent) {
            return new ClimbableBlock();
        }
    }
}
