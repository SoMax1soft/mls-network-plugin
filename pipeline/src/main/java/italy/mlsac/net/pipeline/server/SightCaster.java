// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.lang.ref.WeakReference;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

final class SightCaster {

    private static final boolean IGNORE_PASSABLE_BLOCKS = false;

    private WeakReference<World> boundWorld = new WeakReference<>(null);
    private Location start;
    private final Vector direction = new Vector();

    RayTraceResult cast(World world, SightSegment segment) {
        if (boundWorld.get() != world) {
            boundWorld = new WeakReference<>(world);
            start = new Location(world, 0.0, 0.0, 0.0);
        }
        start.setX(segment.from().x());
        start.setY(segment.from().y());
        start.setZ(segment.from().z());
        direction.setX(segment.directionX());
        direction.setY(segment.directionY());
        direction.setZ(segment.directionZ());
        return world.rayTraceBlocks(start, direction, segment.length(), FluidCollisionMode.NEVER,
                IGNORE_PASSABLE_BLOCKS);
    }
}
