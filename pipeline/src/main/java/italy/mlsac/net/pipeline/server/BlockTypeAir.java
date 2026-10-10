// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.Material;
import org.bukkit.World;

final class BlockTypeAir implements AirReading {

    @Override
    public boolean air(World world, int x, int y, int z) {
        Material type = world.getType(x, y, z);
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }
}
