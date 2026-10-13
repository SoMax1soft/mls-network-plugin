// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.tables.CompactLine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RowRingTest {

    private final List<Long> claims = new ArrayList<>();

    private boolean claim(long bytes) {
        claims.add(bytes);
        return true;
    }

    private static void row(RowRing ring, int value, ChunkClaim claim) {
        ring.stage().integer(value).number(value / 3.0).single(value / 7f).flag(value % 2 == 0).text("ITEM" + value)
                .flag(true).number(-0.0);
        ring.commit(value * 1000L, claim);
    }

    private static List<String> line(RowRing ring, int position) {
        CompactLine line = new CompactLine();
        ring.replay(position, line);
        return line.fields();
    }

    private static List<String> expected(int value) {
        return new CompactLine().integer(value).number(value / 3.0).single(value / 7f).flag(value % 2 == 0)
                .text("ITEM" + value).flag(true).number(-0.0).fields();
    }

    @Test
    void wrapsAroundKeepingTheNewestRowsInOrder() {
        RowRing ring = new RowRing(5, 2);
        for (int value = 1; value <= 12; value++) {
            row(ring, value, this::claim);
        }
        assertEquals(5, ring.size());
        assertEquals(7, ring.dropped());
        for (int position = 0; position < 5; position++) {
            assertEquals(expected(8 + position), line(ring, position));
            assertEquals((8 + position) * 1000L, ring.stampAt(position));
        }
        assertEquals(3, claims.size());
        assertEquals(3, ring.allocatedChunks());
        assertEquals(claims.stream().mapToLong(Long::longValue).sum(), ring.bytes());
    }

    @Test
    void chunksAreClaimedLazilyAndReused() {
        RowRing ring = new RowRing(5000, 250);
        for (int value = 0; value < 300; value++) {
            row(ring, value, this::claim);
        }
        assertEquals(2, ring.allocatedChunks());
        for (int value = 300; value < 12_000; value++) {
            row(ring, value, this::claim);
        }
        assertEquals(20, ring.allocatedChunks());
        assertEquals(20, claims.size());
        assertEquals(expected(7000), line(ring, 0));
        assertEquals(expected(11_999), line(ring, 4999));
    }

    @Test
    void refusedClaimDropsTheRowWithoutGrowing() {
        RowRing ring = new RowRing(10, 4);
        row(ring, 1, bytes -> false);
        assertEquals(0, ring.size());
        assertEquals(0, ring.allocatedChunks());
        row(ring, 2, this::claim);
        row(ring, 3, bytes -> false);
        assertEquals(2, ring.size());
        assertEquals(1, claims.size());
    }

    @Test
    void rowsOfAnotherShapeAreRejected() {
        RowRing ring = new RowRing(10, 4);
        row(ring, 1, this::claim);
        ring.stage().integer(5).integer(6);
        assertFalse(ring.commit(1L, this::claim));
        assertEquals(1, ring.size());
        ring.release();
        assertEquals(0, ring.size());
        assertEquals(0, ring.allocatedChunks());
        assertTrue(ring.bytes() == 0);
    }
}
