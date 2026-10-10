// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.suppression.lag.motion.SetbackTerrain;
import italy.mlsac.net.suppression.motion.WorldPoint;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class BukkitSetbackTerrain implements SetbackTerrain {

    @Override
    public boolean chunkLoaded(WorldPoint position) {
        World world = Bukkit.getWorld(position.world());
        return world != null && world.isChunkLoaded(block(position.x()) >> 4, block(position.z()) >> 4);
    }

    @Override
    public boolean passable(WorldPoint position) {
        World world = Bukkit.getWorld(position.world());
        if (world == null) {
            return false;
        }
        int x = block(position.x());
        int feet = block(position.y());
        int z = block(position.z());
        return world.getBlockAt(x, feet, z).isPassable() && world.getBlockAt(x, feet + 1, z).isPassable();
    }

    private static int block(double coordinate) {
        return (int) Math.floor(coordinate);
    }
}
