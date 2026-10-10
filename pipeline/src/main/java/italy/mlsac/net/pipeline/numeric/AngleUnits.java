// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

public final class AngleUnits {
    private static final double DEGREES_PER_RADIAN = 57.29577951308232;
    private static final double RADIANS_PER_DEGREE = 0.017453292519943295;

    private AngleUnits() {
    }

    public static double degrees(double radians) {
        return radians * DEGREES_PER_RADIAN;
    }

    public static double radians(double degrees) {
        return degrees * RADIANS_PER_DEGREE;
    }
}
