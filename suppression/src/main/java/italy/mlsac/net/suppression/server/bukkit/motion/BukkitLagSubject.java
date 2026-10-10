// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.suppression.lag.runtime.LagActuator;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.motion.WorldPoint;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagSubject;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class BukkitLagSubject implements LagSubject {

    private final Player player;
    private final BodyConditions conditions;
    private final LagActuator actuator;

    BukkitLagSubject(Player player, BodyConditions conditions, LagActuator actuator) {
        this.player = player;
        this.conditions = conditions;
        this.actuator = actuator;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TickFrame frame(long now, long tick) {
        Location at = player.getLocation();
        WorldPoint position = new WorldPoint(player.getWorld().getName(), at.getX(), at.getY(), at.getZ());
        return new TickFrame(now, tick, position, player.isOnGround(), at.getPitch(), player.getFallDistance(),
                conditions.of(player));
    }

    @Override
    public LagActuator actuator() {
        return actuator;
    }
}
