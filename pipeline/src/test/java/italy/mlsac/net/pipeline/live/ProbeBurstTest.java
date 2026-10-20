// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.engine.PipelinePorts;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProbeBurstTest {

    private static final UUID FIRST = new UUID(0xB0B5L, 1L);
    private static final UUID SECOND = new UUID(0xB0B5L, 2L);

    @Test
    void theServerTickSendsEveryViewerProbeInsideOneBurst() {
        ArenaServer arena = new ArenaServer();
        arena.connect(FIRST, 101);
        arena.connect(SECOND, 102);
        List<String> calls = new ArrayList<>();
        DataPipeline pipeline = new DataPipeline(portsWith(arena, new RecordingPings(calls)));
        pipeline.enabled();
        pipeline.gameTick(1);
        pipeline.gameTick(2);
        assertEquals(8, calls.size(), calls.toString());
        for (int burst = 0; burst < 2; burst++) {
            List<String> one = calls.subList(burst * 4, burst * 4 + 4);
            assertEquals("open", one.get(0));
            assertEquals("close", one.get(3));
            assertEquals(Set.of("send " + FIRST, "send " + SECOND), Set.copyOf(one.subList(1, 3)));
        }
    }

    @Test
    void aTickWithoutViewersStillClosesItsBurst() {
        ArenaServer arena = new ArenaServer();
        List<String> calls = new ArrayList<>();
        DataPipeline pipeline = new DataPipeline(portsWith(arena, new RecordingPings(calls)));
        pipeline.enabled();
        pipeline.gameTick(1);
        assertEquals(List.of("open", "close"), calls);
    }

    private static PipelinePorts portsWith(ArenaServer arena, TransactionPings pings) {
        PipelinePorts base = arena.ports();
        return new PipelinePorts(base.states(), base.standing(), new SeededConnections(base.connections()),
                base.rays(), base.monotonic(), base.wallClock(), base.uuids(), base.folder(), base.platform(),
                base.switches(), base.backend(), base.ultraWindows(), base.legacyWindows(), pings);
    }

    private record RecordingPings(List<String> calls) implements TransactionPings {
        @Override
        public void send(UUID viewer, int transactionId) {
            calls.add("send " + viewer);
        }

        @Override
        public void openBurst() {
            calls.add("open");
        }

        @Override
        public void closeBurst() {
            calls.add("close");
        }
    }

    private record SeededConnections(PlayerConnections inner) implements PlayerConnections {
        @Override
        public String clientProtocol(UUID player) {
            return inner.clientProtocol(player);
        }

        @Override
        public String clientLabel(UUID player) {
            return inner.clientLabel(player);
        }

        @Override
        public ViewSeed viewSeed(UUID player) {
            return new ViewSeed("V_1_21_4", ArenaServer.WORLD, new Vec3(0, 64, 0), List.of());
        }

        @Override
        public UUID playerByEntity(int entityNumber) {
            return inner.playerByEntity(entityNumber);
        }

        @Override
        public String name(UUID player) {
            return inner.name(player);
        }
    }
}
