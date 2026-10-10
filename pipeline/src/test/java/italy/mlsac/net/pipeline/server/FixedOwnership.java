// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Entity;

final class FixedOwnership implements ThreadOwnership {

    private final Set<Long> foreignChunks = new HashSet<>();
    private final Set<Entity> foreignEntities = new HashSet<>();

    static FixedOwnership everything() {
        return new FixedOwnership();
    }

    FixedOwnership withoutChunk(int chunkX, int chunkZ) {
        foreignChunks.add(key(chunkX, chunkZ));
        return this;
    }

    FixedOwnership withoutEntity(Entity entity) {
        foreignEntities.add(entity);
        return this;
    }

    @Override
    public boolean ownsChunk(World world, int chunkX, int chunkZ) {
        return !foreignChunks.contains(key(chunkX, chunkZ));
    }

    @Override
    public boolean ownsEntity(Entity entity) {
        return !foreignEntities.contains(entity);
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}
