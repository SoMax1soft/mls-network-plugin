// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.World;

final class BlockTypeAir implements AirReading {

    @Override
    public boolean air(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).isEmpty();
    }
}
