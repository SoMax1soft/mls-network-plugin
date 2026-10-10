// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;

record RayKey(UUID world, double ax, double ay, double az, double bx, double by, double bz) {

    static RayKey of(UUID world, Vec3 from, Vec3 to) {
        return new RayKey(world, from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
    }
}
