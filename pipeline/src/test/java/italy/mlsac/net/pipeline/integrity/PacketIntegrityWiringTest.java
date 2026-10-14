// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.packet.PlayerPositionPacket;
import italy.mlsac.net.packet.TeleportConfirmPacket;
import italy.mlsac.net.pipeline.PipelineAssembly;
import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.live.ServerFacts;
import italy.mlsac.net.pipeline.live.ServerSide;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestCore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PacketIntegrityWiringTest {

    private final UUID player = new UUID(4, 2);

    @Test
    void theAssembledPipelineDropsForgedConfirmsFollowsReloadsAndStops() {
        TestCore core = new TestCore();
        List<BadPacketObserved> reported = new ArrayList<>();
        core.bus.subscribe(BadPacketObserved.class, reported::add);
        PipelineAssembly pipeline = new PipelineAssembly(core.services(),
                new ServerSide(states(), (world, from, to) -> RayVerdict.UNKNOWN, facts()));
        pipeline.start();
        core.packets.send(new PlayerPositionPacket(player, 3));
        assertFalse(core.packets.receive(new TeleportConfirmPacket(player, 3)));
        assertTrue(core.packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        assertEquals(List.of(new BadPacketObserved(player, BadPacketKind.FORGED_TELEPORT_CONFIRM)), reported);
        core.apply(EffectiveConfig.pluginDefaults(TestConfigs.core("bad-packets:\n  enabled: false\n")));
        assertFalse(core.packets.receive(new TeleportConfirmPacket(player, 1_000_001)));
        core.apply(EffectiveConfig.pluginDefaults(TestConfigs.core()));
        assertTrue(core.packets.receive(new TeleportConfirmPacket(player, 1_000_002)));
        pipeline.stop();
        assertFalse(core.packets.receive(new TeleportConfirmPacket(player, 1_000_003)));
        assertEquals(2, reported.size());
    }

    private PlayerStates states() {
        return new PlayerStates() {
            @Override
            public List<UUID> connectedPlayers() {
                return List.of();
            }

            @Override
            public PlayerSample sample(UUID id) {
                return null;
            }

            @Override
            public int serverPing(UUID id) {
                return -1;
            }
        };
    }

    private ServerFacts facts() {
        return new ServerFacts() {
            @Override
            public boolean valid(UUID id) {
                return true;
            }

            @Override
            public ViewSeed viewSeed(UUID id) {
                return null;
            }

            @Override
            public UUID playerByEntity(int entityNumber) {
                return null;
            }
        };
    }
}
