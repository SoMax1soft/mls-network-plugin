// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class FdlibmHypotTest {
    private static final int SAMPLES = 1_000_000;

    @Test
    void platformHypotAgreesWithFdlibmOnGameRangeInputs() {
        SplittableRandom random = new SplittableRandom(2026);
        int differences = 0;
        for (int index = 0; index < SAMPLES; index++) {
            double x = random.nextDouble(-50.0, 50.0);
            double y = random.nextDouble(-50.0, 50.0);
            if (Double.doubleToRawLongBits(Math.hypot(x, y)) != Double.doubleToRawLongBits(StrictMath.hypot(x, y))) {
                differences++;
            }
        }
        assertEquals(0, differences);
    }
}
