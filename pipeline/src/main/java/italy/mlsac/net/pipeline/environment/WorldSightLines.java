// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;

public interface WorldSightLines {
    RayVerdict trace(UUID world, Vec3 from, Vec3 to);

    default boolean provenClear(UUID world, Vec3 from, Box ends) {
        return false;
    }

    default SightRays rays(UUID world, Vec3 eye, Box ends) {
        WorldSightLines lines = this;
        return new SightRays() {
            @Override
            public boolean provenClear() {
                return lines.provenClear(world, eye, ends);
            }

            @Override
            public RayVerdict trace(Vec3 from, Vec3 to) {
                return lines.trace(world, from, to);
            }
        };
    }
}
