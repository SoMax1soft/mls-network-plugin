// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AngleWrapTest {

    @ParameterizedTest
    @CsvSource({
        "0.1, 0.1, 0.09999999999999432, 0.09999999999999432",
        "180.0, 180.0, -180.0, -180.0",
        "-180.0, -180.0, -180.0, -180.0",
        "190.0, -170.0, -170.0, -170.0",
        "540.0, 180.0, -180.0, -180.0",
        "1e-9, 1e-9, 9.999894245993346E-10, 9.999894245993346E-10",
        "45.123456789, 45.123456789, 45.12345678899999, 45.12345678899999"
    })
    void wrapsLikeTheReferenceTable(double angle, double remainder, double floor, double shifted) {
        assertEquals(remainder, AngleWrap.REMAINDER.apply(angle));
        assertEquals(floor, AngleWrap.FLOOR.apply(angle));
        assertEquals(shifted, AngleWrap.SHIFTED_REMAINDER.apply(angle));
    }

    @ParameterizedTest
    @CsvSource({"190.0, -170.0", "-190.0, 170.0", "10.5, 10.5"})
    void wrapsSinglePrecision(float angle, float expected) {
        assertEquals(expected, AngleWrap.remainderSingle(angle));
    }
}
