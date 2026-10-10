// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StreamGateTest {
    private static final UUID PLAYER = new UUID(8, 8);

    @Test
    void cachesTheAllowedVerdictForOneSecondButAlwaysChecksValidity() {
        long[] now = {0L};
        int[] exemptChecks = {0};
        boolean[] exempt = {false};
        boolean[] valid = {true};
        StreamGate gate = new StreamGate(ultraOn(), new PlayerStanding() {
            @Override
            public boolean valid(UUID player) {
                return valid[0];
            }

            @Override
            public boolean exempt(UUID player) {
                exemptChecks[0]++;
                return exempt[0];
            }

            @Override
            public boolean bedrock(UUID player) {
                return false;
            }

            @Override
            public boolean zoneExcluded(UUID player) {
                return false;
            }

            @Override
            public boolean animating(UUID player) {
                return false;
            }
        }, purpose -> now[0]);
        assertTrue(gate.passes(PLAYER));
        exempt[0] = true;
        now[0] = 999_999_999L;
        assertTrue(gate.passes(PLAYER));
        assertEquals(1, exemptChecks[0]);
        now[0] = 1_000_000_000L;
        assertFalse(gate.passes(PLAYER));
        assertEquals(2, exemptChecks[0]);
        exempt[0] = false;
        valid[0] = false;
        now[0] = 3_000_000_000L;
        assertFalse(gate.passes(PLAYER));
    }

    private static DetectionSwitches ultraOn() {
        return new DetectionSwitches() {
            @Override
            public boolean detectionEnabled() {
                return true;
            }

            @Override
            public boolean ultraEnabled() {
                return true;
            }
        };
    }
}
