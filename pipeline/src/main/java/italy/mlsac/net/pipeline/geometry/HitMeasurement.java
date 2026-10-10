// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

public record HitMeasurement(
        boolean valid,
        boolean crosshairOnBox,
        boolean strict,
        int eyeRaysLanded,
        boolean mainEyeHit,
        Vec3 hitRelative,
        double nearMissGap,
        double offsetInHalfWidths,
        double yawOffsetScaled,
        double pitchOffsetScaled,
        double inflation,
        double distance) {

    public static final HitMeasurement INVALID =
            new HitMeasurement(false, false, false, 0, false, Vec3.ZERO, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
}
