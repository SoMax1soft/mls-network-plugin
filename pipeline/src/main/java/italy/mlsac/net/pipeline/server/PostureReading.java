// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.pipeline.player.PlayerPosture;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class PostureReading {

    private final PlayerTrait climbing;
    private final PlayerTrait submerged;
    private final PlayerTrait blind;

    public PostureReading(PlayerTrait climbing, PlayerTrait submerged, PlayerTrait blind) {
        this.climbing = climbing;
        this.submerged = submerged;
        this.blind = blind;
    }

    static PostureReading forRunningServer() {
        PlayerTrait climbing = MethodPresence.declares(LivingEntity.class, "isClimbing")
                ? new NativeClimbing() : new ClimbableUnderfoot();
        PlayerTrait submerged = MethodPresence.declares(Entity.class, "isInWater")
                ? new NativeWaterContact() : new WaterUnderfoot();
        return new PostureReading(climbing, submerged, new Blinded());
    }

    public PlayerPosture read(Player player) {
        return new PlayerPosture(player.isSprinting(), player.isSneaking(), climbing.of(player),
                submerged.of(player), blind.of(player), player.isInsideVehicle());
    }
}
