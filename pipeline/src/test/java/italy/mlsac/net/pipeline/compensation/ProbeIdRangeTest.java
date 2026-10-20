// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProbeIdRangeTest {

    @Test
    void issuedIdsAreRedeemedOnceAndCounted() {
        ProbeIdRange ids = new ProbeIdRange();
        int first = ids.issue();
        int second = ids.issue();
        assertEquals(ProbeIdRange.LOWEST, first);
        assertEquals(ProbeIdRange.LOWEST + 1, second);
        assertEquals(2, ids.notYetFlushed());
        assertTrue(ids.redeem(second));
        assertFalse(ids.redeem(second));
        assertEquals(1, ids.notYetFlushed());
        assertTrue(ids.redeem(first));
        assertEquals(0, ids.notYetFlushed());
    }

    @Test
    void idsNeverIssuedOrOutsideTheRangeAreRefused() {
        ProbeIdRange ids = new ProbeIdRange();
        ids.issue();
        assertFalse(ids.redeem(ProbeIdRange.LOWEST + 5));
        assertFalse(ids.redeem(ProbeIdRange.LOWEST - 1));
        assertFalse(ids.redeem(ProbeIdRange.HIGHEST + 1));
        assertFalse(ids.redeem(-1));
        assertEquals(1, ids.notYetFlushed());
    }

    @Test
    void wrappingAroundReusesAnIdStillOutstandingWithoutCountingItTwice() {
        ProbeIdRange ids = new ProbeIdRange();
        int span = ProbeIdRange.HIGHEST - ProbeIdRange.LOWEST + 1;
        for (int i = 0; i < span; i++) {
            ids.issue();
        }
        assertEquals(span, ids.notYetFlushed());
        assertEquals(ProbeIdRange.LOWEST, ids.issue());
        assertEquals(span, ids.notYetFlushed());
        for (int i = 1; i < span; i++) {
            assertTrue(ids.redeem(ProbeIdRange.LOWEST + i));
        }
        assertEquals(1, ids.notYetFlushed());
    }
}
