// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FightMemoryTest {

    @Test
    void validFromTheHundredthRowAndShareZeroWithoutActions() {
        FightMemory memory = new FightMemory();
        for (int row = 1; row < 100; row++) {
            MemorySnapshot snapshot = memory.record(0, 0);
            assertFalse(snapshot.valid());
            assertEquals(0.0, snapshot.missShare());
        }
        assertTrue(memory.record(0, 0).valid());
    }

    @Test
    void sumsTheLastTwoHundredAndSixHundredRows() {
        FightMemory memory = new FightMemory();
        MemorySnapshot snapshot = null;
        for (int row = 0; row < 700; row++) {
            snapshot = memory.record(1, row % 2);
        }
        assertEquals(200, snapshot.air200());
        assertEquals(100, snapshot.hits200());
        assertEquals(600, snapshot.air600());
        assertEquals(300, snapshot.hits600());
        assertEquals(600.0 / 900.0, snapshot.missShare());
    }
}
