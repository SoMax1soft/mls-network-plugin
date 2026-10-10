// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class ShortestFloatTest {

    private static long bits(double value) {
        return Double.doubleToRawLongBits(value);
    }

    private static void assertSameAsRounding(float value) {
        double expected = ShortestFloat.byRounding(value);
        double actual = ShortestFloat.asWritten(value);
        if (value == 0.0f || (Math.abs(value) < 0x1.0p24f && value == (float) Math.rint(value))) {
            return;
        }
        assertEquals(bits(expected), bits(actual), () -> "value bits " + Integer.toHexString(Float.floatToRawIntBits(value)));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_19)
    void theRuntimeUnderTestTakesTheFastPath() {
        assertTrue(ShortestFloat.SHORTEST_TO_STRING);
    }

    @Test
    void randomBitPatternsMatchTheRoundingSearch() {
        SplittableRandom random = new SplittableRandom(20261006L);
        for (int i = 0; i < 2_000_000; i++) {
            float value = Float.intBitsToFloat(random.nextInt());
            if (Float.isFinite(value)) {
                assertSameAsRounding(value);
            }
        }
    }

    @Test
    void featureRangeValuesMatchTheRoundingSearch() {
        SplittableRandom random = new SplittableRandom(7L);
        for (int i = 0; i < 2_000_000; i++) {
            int magnitude = random.nextInt(0x50000000 - 0x30000000) + 0x30000000;
            float value = Float.intBitsToFloat(random.nextBoolean() ? magnitude | 0x80000000 : magnitude);
            assertSameAsRounding(value);
        }
    }

    @Test
    void neighboursOfEveryPowerOfTwoMatchTheRoundingSearch() {
        for (int exponent = 1; exponent < 255; exponent++) {
            for (int step = -40; step <= 40; step++) {
                float value = Float.intBitsToFloat((exponent << 23) + step);
                if (Float.isFinite(value)) {
                    assertSameAsRounding(value);
                    assertSameAsRounding(-value);
                }
            }
        }
    }

    @Test
    void shortDecimalsFallBackToTheRoundingSearch() {
        float[] values = {0.5f, 0.1f, 0.25f, 1.5f, -2.5f, 3.0e-7f, 1.0e30f, 0.05f};
        for (float value : values) {
            assertSameAsRounding(value);
        }
    }

    @Test
    void nonFiniteAndWholeValuesKeepTheirShortcuts() {
        assertTrue(Double.isNaN(ShortestFloat.asWritten(Float.NaN)));
        assertTrue(Double.isNaN(ShortestFloat.asWritten(Float.POSITIVE_INFINITY)));
        assertEquals(0.0, ShortestFloat.asWritten(-0.0f));
        assertEquals(42.0, ShortestFloat.asWritten(42.0f));
    }
}
