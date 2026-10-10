// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record PlayerPlace(UUID player, int entityNumber, UUID world, Vec3 position) {

    public static PlayerPlace of(Player player) {
        Location at = player.getLocation();
        return new PlayerPlace(player.getUniqueId(), player.getEntityId(), player.getWorld().getUID(),
                new Vec3(at.getX(), at.getY(), at.getZ()));
    }

    public ViewSeed.SeenPlayer seen() {
        return new ViewSeed.SeenPlayer(player, entityNumber, world, position);
    }
}
