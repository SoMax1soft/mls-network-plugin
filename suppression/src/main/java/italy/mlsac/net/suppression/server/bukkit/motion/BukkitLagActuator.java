// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.suppression.lag.runtime.LagActuator;
import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.motion.WorldPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class BukkitLagActuator implements LagActuator {

    private final Player player;
    private final TeleportRoute route;
    private final ViewTurn viewTurn;

    BukkitLagActuator(Player player, TeleportRoute route, ViewTurn viewTurn) {
        this.player = player;
        this.route = route;
        this.viewTurn = viewTurn;
    }

    @Override
    public void setBack(WorldPoint target, float fallDistanceToRestore) {
        Location here = player.getLocation();
        Location back = new Location(player.getWorld(), target.x(), target.y(), target.z(), here.getYaw(),
                here.getPitch());
        route.travel(player, back, () -> player.setFallDistance(fallDistanceToRestore));
    }

    @Override
    public void pushVelocity(Vec3 velocity) {
        player.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z()));
    }

    @Override
    public void turnView(int syncId, double yawDelta, double pitchDelta) {
        viewTurn.turn(player, syncId, yawDelta, pitchDelta);
    }
}
