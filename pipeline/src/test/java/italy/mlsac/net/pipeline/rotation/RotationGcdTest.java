// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RotationGcdTest {

    @Test
    void minimumDivisorIsTheSensitivityFloor() {
        double widened = 0.2f;
        assertEquals(((widened * widened * widened) * 8) * 0.15 - 0.001, RotationGcd.SMALLEST_STEP);
        assertEquals(0.008600000429153449, RotationGcd.SMALLEST_STEP);
    }

    @Test
    void gcdOfMultiplesIsTheStep() {
        assertEquals(0.125, RotationGcd.of(0.375, 0.25));
        assertEquals(0.5, RotationGcd.of(0.5, 0.0));
        assertEquals(0.0, RotationGcd.of(0.0, 0.25));
    }
}
