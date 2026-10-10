// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import org.bukkit.World;

record AirVolume(int lowX, int lowY, int lowZ, int highX, int highY, int highZ) {

    static final double EDGE_SLACK = 1.0e-4;
    static final int MOST_BLOCKS = 48;

    static boolean spannable(Vec3 from, Box ends) {
        return Double.isFinite(from.x()) && Double.isFinite(from.y()) && Double.isFinite(from.z())
                && Double.isFinite(ends.low().x()) && Double.isFinite(ends.low().y())
                && Double.isFinite(ends.low().z()) && Double.isFinite(ends.high().x())
                && Double.isFinite(ends.high().y()) && Double.isFinite(ends.high().z());
    }

    static AirVolume spanning(Vec3 from, Box ends) {
        return new AirVolume(
                lowBlock(Math.min(from.x(), ends.low().x())), lowBlock(Math.min(from.y(), ends.low().y())),
                lowBlock(Math.min(from.z(), ends.low().z())), highBlock(Math.max(from.x(), ends.high().x())),
                highBlock(Math.max(from.y(), ends.high().y())), highBlock(Math.max(from.z(), ends.high().z())));
    }

    boolean small() {
        long blocks = (long) (highX - lowX + 1) * (highY - lowY + 1) * (highZ - lowZ + 1);
        return blocks <= MOST_BLOCKS;
    }

    boolean readable(World world, ThreadOwnership ownership) {
        for (int chunkX = lowX >> 4; chunkX <= highX >> 4; chunkX++) {
            for (int chunkZ = lowZ >> 4; chunkZ <= highZ >> 4; chunkZ++) {
                if (!ownership.ownsChunk(world, chunkX, chunkZ) || !world.isChunkLoaded(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean onlyAir(World world, AirReading reading) {
        for (int y = lowY; y <= highY; y++) {
            for (int x = lowX; x <= highX; x++) {
                for (int z = lowZ; z <= highZ; z++) {
                    if (!reading.air(world, x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int lowBlock(double coordinate) {
        return (int) Math.floor(coordinate - EDGE_SLACK);
    }

    private static int highBlock(double coordinate) {
        return (int) Math.floor(coordinate + EDGE_SLACK);
    }
}
