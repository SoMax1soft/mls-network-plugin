// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import italy.mlsac.net.pipeline.rotation.RotationGcd;
import italy.mlsac.net.pipeline.rotation.StepFrequencyTable;

final class LegacyAxis {
    private static final double BUCKET_WIDTH = 0.001;
    private static final float MAXIMUM_MAGNITUDE = 5.0f;
    private static final int MINIMUM_HELD = 15;

    private final StepFrequencyTable divisors = new StepFrequencyTable();
    private float lastMagnitude;
    private double mode;
    private float previousDelta;
    private float previousAccel;
    private float currentAccel;

    void reset() {
        divisors.clear();
        lastMagnitude = 0f;
        mode = 0.0;
        previousDelta = 0f;
        previousAccel = 0f;
        currentAccel = 0f;
    }

    void restart() {
        previousDelta = 0f;
        previousAccel = 0f;
        currentAccel = 0f;
    }

    double mode() {
        return mode;
    }

    float[] step(float delta) {
        previousAccel = currentAccel;
        currentAccel = delta - previousDelta;
        float jerk = currentAccel - previousAccel;
        float magnitude = Math.abs(delta);
        double divisor = RotationGcd.of(magnitude, lastMagnitude);
        if (magnitude > 0 && magnitude < MAXIMUM_MAGNITUDE && divisor > RotationGcd.SMALLEST_STEP) {
            divisors.add(divisor, BUCKET_WIDTH);
            lastMagnitude = magnitude;
        }
        if (divisors.size() > MINIMUM_HELD) {
            StepFrequencyTable.Peak popular = divisors.mostPopular();
            if (popular.count() > MINIMUM_HELD) {
                mode = popular.key();
            }
        }
        float divisorResidue = 0f;
        if (mode != 0) {
            double rest = (double) magnitude % mode;
            divisorResidue = (float) Math.min(rest, mode - rest);
        }
        previousDelta = delta;
        return new float[] {currentAccel, jerk, divisorResidue};
    }
}
