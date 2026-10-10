// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberTextTest {

    @Test
    void compactFormatMatchesTheReferenceTable() {
        assertEquals("0.11999999731779099", CompactNumberText.of((double) 0.12f));
        assertEquals("10000000", CompactNumberText.of(1.0E7));
        assertEquals("1.0E15", CompactNumberText.of(1.0E15));
        assertEquals("-7.239835792942628E-4", CompactNumberText.of(-7.239835792942628E-4));
        assertEquals("0", CompactNumberText.of(Double.NaN));
        assertEquals("0", CompactNumberText.of(-0.0));
        assertEquals("-1", CompactNumberText.of(-1.0));
        assertEquals("50", CompactNumberText.of(50.0));
        assertEquals("-37.568283", CompactNumberText.of(-37.568283f));
        assertEquals("1", CompactNumberText.of(1.0f));
        assertEquals("16777216", CompactNumberText.of(16777217f));
        assertEquals("0", CompactNumberText.of(Float.NaN));
        assertEquals("1", CompactNumberText.of(true));
    }

    @Test
    void plainFormatKeepsJavaText() {
        assertEquals("0.0", PlainNumberText.of(0.0));
        assertEquals("-0.0", PlainNumberText.of(-0.0));
        assertEquals("-1.0", PlainNumberText.of(-1.0));
        assertEquals("50.0", PlainNumberText.of(50.0));
        assertEquals("NaN", PlainNumberText.of(Double.NaN));
        assertEquals("23.23518", PlainNumberText.of(23.23518f));
        assertEquals("1.6777216E7", PlainNumberText.of(1.6777216E7f));
        assertEquals("9.99E-4", PlainNumberText.of(9.99E-4));
    }

    @Test
    void sixDigitFormatRoundsHalfUp() {
        assertEquals("-12.601563", SixDigitNumberText.of(-12.6015625f));
        assertEquals("0.000000", SixDigitNumberText.of(0.0f));
        assertEquals("-0.000000", SixDigitNumberText.of(-0.0f));
        assertEquals("0.000000", SixDigitNumberText.of(1.0E-7f));
    }
}
