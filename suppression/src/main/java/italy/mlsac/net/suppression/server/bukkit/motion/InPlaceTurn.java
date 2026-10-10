// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class InPlaceTurn implements ViewTurn {

    private static final float LOWEST_PITCH = -90.0f;
    private static final float HIGHEST_PITCH = 90.0f;

    private final TeleportRoute route;

    public InPlaceTurn(TeleportRoute route) {
        this.route = route;
    }

    @Override
    public void turn(Player player, int syncId, double yawDelta, double pitchDelta) {
        Location here = player.getLocation();
        float fallBlocks = player.getFallDistance();
        here.setYaw((float) (here.getYaw() + yawDelta));
        here.setPitch(Math.max(LOWEST_PITCH, Math.min(HIGHEST_PITCH, (float) (here.getPitch() + pitchDelta))));
        route.travel(player, here, () -> player.setFallDistance(fallBlocks));
    }
}
