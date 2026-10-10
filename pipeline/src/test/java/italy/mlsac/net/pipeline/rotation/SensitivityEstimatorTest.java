// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class SensitivityEstimatorTest {
    private static final double STEP = 0.125;

    @Test
    void yawModeConvergesToTheMouseStep() {
        RotationChain chain = new RotationChain();
        SplittableRandom random = new SplittableRandom(7);
        float yaw = 10f;
        float pitch = 5f;
        RotationStep step = chain.rotate(yaw, pitch);
        boolean validEarly = false;
        for (int index = 0; index < 400; index++) {
            yaw += (float) ((1 + random.nextInt(4)) * STEP * (random.nextBoolean() ? 1 : -1));
            pitch += (float) ((1 + random.nextInt(3)) * STEP * (random.nextBoolean() ? 1 : -1));
            step = chain.rotate(yaw, pitch);
            if (index < 16) {
                validEarly |= step.yawSensKnown();
            }
        }
        assertFalse(validEarly);
        assertTrue(step.yawSensKnown());
        assertTrue(step.pitchSensKnown());
        assertEquals(STEP, step.yawSensStep(), 1e-12);
        assertEquals(STEP, step.pitchSensStep(), 1e-12);
    }

    @Test
    void firstDivisorAgainstZeroIsNotCounted() {
        SensitivityAxis axis = new SensitivityAxis(new PitchDivisorRule());
        axis.absorb(0.375, 1e-6f);
        assertEquals(0, axis.histogramSize());
        axis.absorb(0.25, 1e-6f);
        assertEquals(1, axis.histogramSize());
    }

    @Test
    void chainBreakKeepsTheEstimator() {
        RotationChain chain = new RotationChain();
        float yaw = 0f;
        chain.rotate(yaw, 0f);
        for (int index = 0; index < 300; index++) {
            yaw += (float) (((index % 3) + 1) * STEP);
            chain.rotate(yaw, (float) ((index % 2) * STEP));
        }
        chain.severRotation();
        RotationStep first = chain.rotate(yaw, 0f);
        assertTrue(first.present());
        assertFalse(first.valid());
        assertTrue(first.yawSensKnown());
        assertEquals(0.0, first.yawTurn());
    }
}
