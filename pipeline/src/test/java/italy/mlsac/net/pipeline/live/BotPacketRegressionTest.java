// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.*;

import italy.mlsac.net.packet.TeleportConfirmPacket;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.FakePacketLayer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BotPacketRegressionTest {
    @Test
    void fakeTeleportTrafficCannotStarveTwentyFourFightingPlayers() {
        List<List<byte[]>> baseline = fight(false);
        List<List<byte[]>> attacked = fight(true);
        for (int player = 0; player < baseline.size(); player++) {
            assertFalse(attacked.get(player).isEmpty());
            assertEquals(baseline.get(player).size(), attacked.get(player).size());
            for (int window = 0; window < baseline.get(player).size(); window++) {
                assertArrayEquals(baseline.get(player).get(window), attacked.get(player).get(window));
            }
        }
    }

    private List<List<byte[]>> fight(boolean spoof) {
        ArenaServer server = new ArenaServer();
        ArenaFight arena = new ArenaFight(server, 24);
        PlayerOrderedInput input = new PlayerOrderedInput(new DataPipeline(server.ports()));
        FakePacketLayer packets = new FakePacketLayer(GameVersion.parse("1.21.4").orElseThrow());
        PacketFeed feed = new PacketFeed(packets, input);
        feed.start();
        input.enabled();
        for (long tick = 0; tick < 360; tick++) {
            arena.tickServer(input, tick);
            for (int index = 0; index < arena.players().size(); index++) {
                UUID player = arena.players().get(index);
                if (spoof) {
                    packets.receive(new TeleportConfirmPacket(player, 1000000 + (int) tick));
                    input.movement(new MovementPacket(player, tick * ArenaFight.TICK_NANOS + 1,
                            Float.NaN, 0, true, 0, 0, 0, false, true, false));
                }
                arena.tickClient(input, index, tick);
            }
        }
        feed.stop();
        return arena.players().stream().map(player -> List.copyOf(server.windowsOf(player))).toList();
    }
}
