// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.environment.SightRays;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class SightBundle implements SightRays {

    static final int MOST_SHARED_BLOCKS = 512;
    private static final double EDGE_NUDGE = -1.0E-7;
    private static final byte UNREAD = 0;
    private static final byte OPEN = 1;
    private static final byte FILLED = 2;
    private static final RayTraceResult OUTSIDE = new RayTraceResult(new Vector());

    private final BukkitSightLines lines;
    private final UUID worldId;
    private final World world;
    private final AirVolume volume;
    private final AirReading air;
    private final PluginLog log;
    private final byte[] cells;
    private final int sizeX;
    private final int sizeZ;
    private final Location start;
    private final Vector direction = new Vector();

    SightBundle(BukkitSightLines lines, UUID worldId, World world, AirVolume volume, AirReading air, PluginLog log) {
        this.lines = lines;
        this.worldId = worldId;
        this.world = world;
        this.volume = volume;
        this.air = air;
        this.log = log;
        this.sizeX = volume.highX() - volume.lowX() + 1;
        this.sizeZ = volume.highZ() - volume.lowZ() + 1;
        this.cells = new byte[sizeX * sizeZ * (volume.highY() - volume.lowY() + 1)];
        this.start = new Location(world, 0.0, 0.0, 0.0);
    }

    static long blocks(AirVolume volume) {
        return (long) (volume.highX() - volume.lowX() + 1) * (volume.highY() - volume.lowY() + 1)
                * (volume.highZ() - volume.lowZ() + 1);
    }

    @Override
    public boolean provenClear() {
        if (!volume.small()) {
            return false;
        }
        try {
            for (int y = volume.lowY(); y <= volume.highY(); y++) {
                for (int x = volume.lowX(); x <= volume.highX(); x++) {
                    for (int z = volume.lowZ(); z <= volume.highZ(); z++) {
                        if (!open(x, y, z)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (RuntimeException failure) {
            log.debug(LogTopic.DATA, () -> "Open-air proof unavailable, asking each sight line: " + failure);
            return false;
        }
    }

    @Override
    public RayVerdict trace(Vec3 from, Vec3 to) {
        if (!SightSegment.finite(from, to)) {
            return RayVerdict.UNKNOWN;
        }
        try {
            SightSegment segment = SightSegment.between(from, to);
            if (segment.tooShort()) {
                return RayVerdict.CLEAR;
            }
            RayTraceResult hit = cast(segment);
            if (hit == OUTSIDE) {
                return lines.trace(worldId, from, to);
            }
            return BukkitSightLines.verdictOf(hit, segment);
        } catch (RuntimeException failure) {
            log.warn(LogTopic.DATA, "A sight-line check failed: " + failure.getClass().getSimpleName(), failure);
            return RayVerdict.UNKNOWN;
        }
    }

    RayTraceResult cast(SightSegment segment) {
        double fromX = segment.from().x();
        double fromY = segment.from().y();
        double fromZ = segment.from().z();
        double headingX = segment.directionX();
        double headingY = segment.directionY();
        double headingZ = segment.directionZ();
        double reach = segment.length();
        double headingLength = Math.sqrt(headingX * headingX + headingY * headingY + headingZ * headingZ);
        double stepX = headingX / headingLength * reach;
        double stepY = headingY / headingLength * reach;
        double stepZ = headingZ / headingLength * reach;
        double toX = fromX + stepX;
        double toY = fromY + stepY;
        double toZ = fromZ + stepZ;
        start.setX(fromX);
        start.setY(fromY);
        start.setZ(fromZ);
        direction.setX(headingX);
        direction.setY(headingY);
        direction.setZ(headingZ);
        if (fromX == toX && fromY == toY && fromZ == toZ) {
            return null;
        }
        double endX = lerp(toX, fromX);
        double endY = lerp(toY, fromY);
        double endZ = lerp(toZ, fromZ);
        double beginX = lerp(fromX, toX);
        double beginY = lerp(fromY, toY);
        double beginZ = lerp(fromZ, toZ);
        int x = floor(beginX);
        int y = floor(beginY);
        int z = floor(beginZ);
        RayTraceResult first = visit(x, y, z, reach);
        if (first != null) {
            return first;
        }
        double spanX = endX - beginX;
        double spanY = endY - beginY;
        double spanZ = endZ - beginZ;
        int signX = sign(spanX);
        int signY = sign(spanY);
        int signZ = sign(spanZ);
        double deltaX = signX == 0 ? Double.MAX_VALUE : (double) signX / spanX;
        double deltaY = signY == 0 ? Double.MAX_VALUE : (double) signY / spanY;
        double deltaZ = signZ == 0 ? Double.MAX_VALUE : (double) signZ / spanZ;
        double nextX = deltaX * (signX > 0 ? 1.0 - frac(beginX) : frac(beginX));
        double nextY = deltaY * (signY > 0 ? 1.0 - frac(beginY) : frac(beginY));
        double nextZ = deltaZ * (signZ > 0 ? 1.0 - frac(beginZ) : frac(beginZ));
        while (nextX <= 1.0 || nextY <= 1.0 || nextZ <= 1.0) {
            if (nextX < nextY) {
                if (nextX < nextZ) {
                    x += signX;
                    nextX += deltaX;
                } else {
                    z += signZ;
                    nextZ += deltaZ;
                }
            } else if (nextY < nextZ) {
                y += signY;
                nextY += deltaY;
            } else {
                z += signZ;
                nextZ += deltaZ;
            }
            RayTraceResult hit = visit(x, y, z, reach);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private RayTraceResult visit(int x, int y, int z, double reach) {
        if (x < volume.lowX() || x > volume.highX() || y < volume.lowY() || y > volume.highY()
                || z < volume.lowZ() || z > volume.highZ()) {
            return OUTSIDE;
        }
        if (open(x, y, z)) {
            return null;
        }
        return world.getBlockAt(x, y, z).rayTrace(start, direction, reach, FluidCollisionMode.NEVER);
    }

    private boolean open(int x, int y, int z) {
        int index = ((y - volume.lowY()) * sizeX + (x - volume.lowX())) * sizeZ + (z - volume.lowZ());
        byte known = cells[index];
        if (known == UNREAD) {
            known = air.air(world, x, y, z) ? OPEN : FILLED;
            cells[index] = known;
        }
        return known == OPEN;
    }

    private static double lerp(double start, double end) {
        return start + EDGE_NUDGE * (end - start);
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private static long longFloor(double value) {
        long truncated = (long) value;
        return value < truncated ? truncated - 1L : truncated;
    }

    private static double frac(double value) {
        return value - (double) longFloor(value);
    }

    private static int sign(double value) {
        if (value == 0.0) {
            return 0;
        }
        return value > 0.0 ? 1 : -1;
    }
}
