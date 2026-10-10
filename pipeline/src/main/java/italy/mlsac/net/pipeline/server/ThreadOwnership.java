// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.World;
import org.bukkit.entity.Entity;

public interface ThreadOwnership {

    boolean ownsChunk(World world, int chunkX, int chunkZ);

    boolean ownsEntity(Entity entity);
}
