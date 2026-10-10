// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

import italy.mlsac.net.pipeline.numeric.AngleUnits;

public final class LookDirection {
    private LookDirection() {
    }

    public static Vec3 of(double yaw, double pitch) {
        double yawRadians = AngleUnits.radians(yaw);
        double pitchRadians = AngleUnits.radians(pitch);
        double pitchCos = Math.cos(pitchRadians);
        return new Vec3(-Math.sin(yawRadians) * pitchCos, -Math.sin(pitchRadians), Math.cos(yawRadians) * pitchCos);
    }
}
