// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PositionHistoryTest {
    private static final UUID PLAYER = new UUID(1, 1);
    private static final UUID WORLD = new UUID(2, 2);

    private static PlayerSample at(long nanos, double x) {
        return new PlayerSample(PLAYER, WORLD, 7, nanos, x, 64.0, 0.0, 10f, 5f, 1.62, 1.8, 0.6, 0.5, 0.5f, true, false,
                new PlayerPosture(true, false, false, false, false, false), "stick", 12.5f, "SURVIVAL", null);
    }

    @Test
    void exactHitReturnsTheStoredSample() {
        PositionHistory history = new PositionHistory();
        PlayerSample first = at(100, 0.0);
        history.append(first);
        history.append(at(200, 1.0));
        assertSame(first, history.at(100).orElseThrow());
    }

    @Test
    void interpolatesBetweenContinuousSamples() {
        PositionHistory history = new PositionHistory();
        history.append(at(100, 0.0));
        history.append(at(200, 1.0));
        PlayerSample quarter = history.at(125).orElseThrow();
        assertEquals(0.25, quarter.x());
        assertEquals(125, quarter.capturedAt());
        assertEquals("unknown", quarter.handMaterial());
        assertFalse(quarter.posture().sprinting());
        assertEquals(-1.0, quarter.reach());
        assertEquals(-1.0, quarter.swingsPerSecond());
        assertEquals(0.75, history.at(175).orElseThrow().x());
    }

    @Test
    void discontinuousPairReturnsTheNearerSample() {
        PositionHistory history = new PositionHistory();
        PlayerSample before = at(100, 0.0);
        PlayerSample after = at(200, 5.0);
        history.append(before);
        history.append(after);
        assertSame(before, history.at(120).orElseThrow());
        assertSame(after, history.at(160).orElseThrow());
    }

    @Test
    void queriesOutsideTheStoredRange() {
        PositionHistory history = new PositionHistory();
        history.append(at(100, 0.0));
        PlayerSample newest = at(200, 1.0);
        history.append(newest);
        assertFalse(history.at(50).isPresent());
        assertSame(newest, history.at(900).orElseThrow());
        history.append(at(150, 3.0));
        assertEquals(1, history.between(100, 250).size());
    }
}
