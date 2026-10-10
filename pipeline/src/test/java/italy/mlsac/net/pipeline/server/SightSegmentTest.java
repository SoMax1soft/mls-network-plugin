// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.geometry.Vec3;
import org.junit.jupiter.api.Test;

class SightSegmentTest {

    @Test
    void chunkIndexFloorsBeforeShifting() {
        assertEquals(0, SightSegment.chunkIndex(15.99));
        assertEquals(1, SightSegment.chunkIndex(16.0));
        assertEquals(-1, SightSegment.chunkIndex(-0.5));
        assertEquals(-1, SightSegment.chunkIndex(-16.0));
        assertEquals(-2, SightSegment.chunkIndex(-16.01));
    }

    @Test
    void shortSegmentsAreNeverCut() {
        assertTrue(SightSegment.between(new Vec3(1, 2, 3), new Vec3(1, 2, 3.0009)).tooShort());
        assertFalse(SightSegment.between(new Vec3(1, 2, 3), new Vec3(1, 2, 3.0011)).tooShort());
    }

    @Test
    void hitsWithinTheLastMillimetreDoNotCount() {
        assertEquals(5.0, SightSegment.between(new Vec3(0, 0, 0), new Vec3(3, 4, 0)).length(), 1e-12);
        SightSegment segment = SightSegment.between(new Vec3(0, 2, 3), new Vec3(5, 2, 3));
        assertTrue(segment.cutAt(4.998, 2.0, 3.0));
        assertFalse(segment.cutAt(4.999, 2.0, 3.0));
        assertFalse(segment.cutAt(5.0, 2.0, 3.0));
    }

    @Test
    void directionIsUnitLength() {
        SightSegment segment = SightSegment.between(new Vec3(1, 1, 1), new Vec3(4, 5, 1));
        assertEquals(0.6, segment.directionX(), 1e-12);
        assertEquals(0.8, segment.directionY(), 1e-12);
        assertEquals(0.0, segment.directionZ(), 1e-12);
    }

    @Test
    void nonFiniteEndsAreReported() {
        assertFalse(SightSegment.finite(new Vec3(Double.NaN, 0, 0), new Vec3(1, 1, 1)));
        assertFalse(SightSegment.finite(new Vec3(0, 0, 0), new Vec3(1, Double.POSITIVE_INFINITY, 1)));
        assertTrue(SightSegment.finite(new Vec3(0, 0, 0), new Vec3(1, 1, 1)));
    }
}
