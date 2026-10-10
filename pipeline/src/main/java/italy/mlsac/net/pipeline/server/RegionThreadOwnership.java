// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

final class RegionThreadOwnership implements ThreadOwnership {

    private static final String OWNERSHIP_QUERY = "isOwnedByCurrentRegion";

    private final Optional<MethodHandle> chunkQuery;
    private final Optional<MethodHandle> entityQuery;

    RegionThreadOwnership() {
        this.chunkQuery = serverQuery(MethodType.methodType(boolean.class, World.class, int.class, int.class));
        this.entityQuery = serverQuery(MethodType.methodType(boolean.class, Entity.class));
    }

    @Override
    public boolean ownsChunk(World world, int chunkX, int chunkZ) {
        if (chunkQuery.isEmpty()) {
            return true;
        }
        try {
            return (boolean) chunkQuery.get().invokeExact(world, chunkX, chunkZ);
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Override
    public boolean ownsEntity(Entity entity) {
        if (entityQuery.isEmpty()) {
            return true;
        }
        try {
            return (boolean) entityQuery.get().invokeExact(entity);
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Optional<MethodHandle> serverQuery(MethodType type) {
        try {
            return Optional.of(MethodHandles.publicLookup().findStatic(Bukkit.class, OWNERSHIP_QUERY, type));
        } catch (NoSuchMethodException | IllegalAccessException absent) {
            return Optional.empty();
        }
    }
}
