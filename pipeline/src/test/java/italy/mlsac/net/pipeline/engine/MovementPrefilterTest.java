// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import static org.junit.jupiter.api.Assertions.*;

import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.player.VersionNames;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovementPrefilterTest {
    private final UUID player = new UUID(1, 2);

    @Test
    void sameSpotPositionLookIsDroppedOnlyForEchoingClientVersions() {
        for (String version : new String[] {"V_1_17", "V_1_18_2", "V_1_19_4", "V_1_20_5"}) {
            MovementPrefilter filter = filterFor(version);
            assertTrue(filter.accepts(move(0, 0, 0, 0, false)), version);
            assertFalse(filter.accepts(move(50_000_000, 10, 5, 0, false)), version);
            assertTrue(filter.accepts(move(100_000_000, 10, 5, 0.5, false)), version);
        }
        for (String version : new String[] {"V_1_16_4", "V_1_21", "V_1_21_4"}) {
            MovementPrefilter filter = filterFor(version);
            assertTrue(filter.accepts(move(0, 0, 0, 0, false)), version);
            assertTrue(filter.accepts(move(50_000_000, 10, 5, 0, false)), version);
        }
    }

    @Test
    void cancelledMovementNeitherIsDroppedNorMovesTheRememberedSpot() {
        MovementPrefilter filter = filterFor("V_1_20_3");
        assertTrue(filter.accepts(move(0, 0, 0, 0, false)));
        assertTrue(filter.accepts(move(50_000_000, 0, 0, 1000, true)));
        assertFalse(filter.accepts(move(100_000_000, 3, 0, 0, false)));
    }

    private MovementPrefilter filterFor(String version) {
        PlayerConnections clients = (PlayerConnections) Proxy.newProxyInstance(PlayerConnections.class.getClassLoader(),
                new Class<?>[] {PlayerConnections.class}, (proxy, method, args) -> version);
        return new MovementPrefilter(clients, new VersionNames());
    }

    private MovementPacket move(long nanos, float yaw, float pitch, double x, boolean cancelled) {
        return new MovementPacket(player, nanos, yaw, pitch, true, x, 64, 0, true, true, cancelled);
    }
}
