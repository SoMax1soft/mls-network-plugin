// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DisplayNumbersTest {

    @Test
    void percentVectors() {
        assertEquals("95", DisplayNumbers.percent(0.95));
        assertEquals("95.3", DisplayNumbers.percent(0.953));
        assertEquals("95.7", DisplayNumbers.percent(0.9567));
        assertEquals("100", DisplayNumbers.percent(1.0));
        assertEquals("0", DisplayNumbers.percent(0.00004));
    }

    @Test
    void otherFormats() {
        assertEquals("99", DisplayNumbers.truncatedPercent(0.999));
        assertEquals("0.96", DisplayNumbers.twoDecimals(0.9567));
        assertEquals("20.00", DisplayNumbers.twoDecimals(20));
        assertEquals("13.2", DisplayNumbers.oneDecimal(13.2));
        assertEquals("76", DisplayNumbers.roundedPercent(0.7607));
    }
}
