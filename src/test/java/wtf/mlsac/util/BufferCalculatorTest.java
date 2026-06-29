package wtf.mlsac.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BufferCalculatorTest {

    private static final double EPS = 1e-9;
    private static final double DECREASE_THRESHOLD = 0.10;
    private static final double MULTIPLIER = 100.0;
    private static final double INCREASE_THRESHOLD = 0.80;
    private static final double MAX_DECAY = 6.0; // buffer removed at probability 0

    @Test
    void scaledDecreaseIsZeroAtThresholdAndMaxAtZero() {
        assertEquals(0.0, BufferCalculator.scaledDecrease(DECREASE_THRESHOLD, MAX_DECAY, DECREASE_THRESHOLD), EPS);
        assertEquals(MAX_DECAY, BufferCalculator.scaledDecrease(0.0, MAX_DECAY, DECREASE_THRESHOLD), EPS);
    }

    @Test
    void scaledDecreaseScalesLinearly() {
        // Halfway below the threshold -> half of the max decay.
        assertEquals(MAX_DECAY * 0.5, BufferCalculator.scaledDecrease(0.05, MAX_DECAY, DECREASE_THRESHOLD), EPS);
        assertEquals(MAX_DECAY * 0.8, BufferCalculator.scaledDecrease(0.02, MAX_DECAY, DECREASE_THRESHOLD), EPS);
    }

    @Test
    void updateBufferAppliesScaledDecayBelowThreshold() {
        // prob 0 -> max decay (6)
        assertEquals(10.0 - 6.0,
                BufferCalculator.updateBuffer(10.0, 0.0, MULTIPLIER, MAX_DECAY, INCREASE_THRESHOLD, DECREASE_THRESHOLD),
                EPS);
        // prob 0.05 -> 3.0 decay
        assertEquals(10.0 - 3.0,
                BufferCalculator.updateBuffer(10.0, 0.05, MULTIPLIER, MAX_DECAY, INCREASE_THRESHOLD, DECREASE_THRESHOLD),
                EPS);
    }

    @Test
    void updateBufferNeverGoesNegative() {
        assertEquals(0.0,
                BufferCalculator.updateBuffer(4.0, 0.0, MULTIPLIER, MAX_DECAY, INCREASE_THRESHOLD, DECREASE_THRESHOLD),
                EPS);
    }

    @Test
    void updateBufferIncreasesAboveThresholdAndHoldsInBetween() {
        // Gain unchanged: grows by (prob - increaseThreshold) * multiplier above the alert threshold.
        assertEquals(10.0 + (0.90 - INCREASE_THRESHOLD) * MULTIPLIER,
                BufferCalculator.updateBuffer(10.0, 0.90, MULTIPLIER, MAX_DECAY, INCREASE_THRESHOLD, DECREASE_THRESHOLD),
                EPS);
        // Between decay threshold and alert threshold -> unchanged.
        assertEquals(10.0,
                BufferCalculator.updateBuffer(10.0, 0.50, MULTIPLIER, MAX_DECAY, INCREASE_THRESHOLD, DECREASE_THRESHOLD),
                EPS);
    }
}
