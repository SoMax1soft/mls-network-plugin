// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

public final class RotationGcd {
    public static final double SMALLEST_STEP = 0.008600000429153449;

    private RotationGcd() {
    }

    public static double of(double first, double second) {
        if (first == 0) {
            return 0;
        }
        double larger = first >= second ? first : second;
        double smaller = first >= second ? second : first;
        while (smaller > SMALLEST_STEP) {
            double rest = larger - Math.floor(larger / smaller) * smaller;
            larger = smaller;
            smaller = rest;
        }
        return larger;
    }
}
