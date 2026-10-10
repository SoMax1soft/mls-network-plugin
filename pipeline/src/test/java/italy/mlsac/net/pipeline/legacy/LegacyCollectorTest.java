// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LegacyCollectorTest {
    private static final UUID PLAYER = new UUID(5, 5);

    @Test
    void emitsEveryTenRotationsOnceFortyAreHeldAndTeleportResets() {
        List<byte[]> windows = new ArrayList<>();
        LegacyCollector collector = new LegacyCollector(legacySwitches(), usableBackend(), cleanStanding(),
                (player, window) -> windows.add(window));
        collector.attackedPlayer(PLAYER);
        for (int rotation = 0; rotation < 40; rotation++) {
            collector.rotation(PLAYER, rotation * 0.5f, 1f);
        }
        assertEquals(1, windows.size());
        for (int rotation = 0; rotation < 10; rotation++) {
            collector.rotation(PLAYER, 20f + rotation * 0.5f, 1f);
        }
        assertEquals(2, windows.size());
        collector.teleported(PLAYER);
        collector.rotation(PLAYER, 90f, 1f);
        List<LegacyDelta> history = collector.history(PLAYER);
        assertEquals(51, history.size());
        assertEquals(LegacyDelta.ZERO, history.get(50));
    }

    @Test
    void combatTimesOutAfterFortyTicks() {
        List<byte[]> windows = new ArrayList<>();
        LegacyCollector collector = new LegacyCollector(legacySwitches(), usableBackend(), cleanStanding(),
                (player, window) -> windows.add(window));
        collector.attackedPlayer(PLAYER);
        for (int tick = 0; tick < 41; tick++) {
            collector.gameTick(PLAYER);
        }
        collector.rotation(PLAYER, 1f, 1f);
        assertEquals(0, collector.history(PLAYER).size());
    }

    private static DetectionSwitches legacySwitches() {
        return new DetectionSwitches() {
            @Override
            public boolean detectionEnabled() {
                return true;
            }

            @Override
            public boolean ultraEnabled() {
                return false;
            }
        };
    }

    private static BackendAvailability usableBackend() {
        return new BackendAvailability() {
            @Override
            public boolean clientPresent() {
                return true;
            }

            @Override
            public boolean usable() {
                return true;
            }
        };
    }

    private static PlayerStanding cleanStanding() {
        return new PlayerStanding() {
            @Override
            public boolean valid(UUID player) {
                return true;
            }

            @Override
            public boolean exempt(UUID player) {
                return false;
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
        };
    }
}
