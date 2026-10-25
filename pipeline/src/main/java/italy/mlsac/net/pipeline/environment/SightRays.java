// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import italy.mlsac.net.pipeline.geometry.Vec3;

public interface SightRays {

    boolean provenClear();

    RayVerdict trace(Vec3 from, Vec3 to);
}
