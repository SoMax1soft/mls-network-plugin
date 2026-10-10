// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

final class MainThreadOwnership implements ThreadOwnership {

    @Override
    public boolean ownsChunk(World world, int chunkX, int chunkZ) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean ownsEntity(Entity entity) {
        return Bukkit.isPrimaryThread();
    }
}
