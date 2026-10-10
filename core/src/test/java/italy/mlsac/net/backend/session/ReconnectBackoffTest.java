// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ReconnectBackoffTest {

    private final ReconnectBackoff backoff = new ReconnectBackoff();

    @Test
    void baseDelaysDoubleUpToFiveMinutes() {
        List<Long> seconds = IntStream.range(0, 8).mapToObj(attempt -> backoff.baseMillis(attempt) / 1000).toList();
        assertEquals(List.of(10L, 20L, 40L, 80L, 160L, 300L, 300L, 300L), seconds);
    }

    @Test
    void jitterBoundIsAFifthPlusOne() {
        assertEquals(2001L, backoff.jitterBound(10_000));
        assertEquals(4001L, backoff.jitterBound(20_000));
        assertEquals(60_001L, backoff.jitterBound(300_000));
    }

    @Test
    void delaysBecomeTicks() {
        assertEquals(200L, backoff.delayTicks(10_000));
        assertEquals(240L, backoff.delayTicks(12_000));
        assertEquals(1L, backoff.delayTicks(10));
    }
}
