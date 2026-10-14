// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.PlayerPositionPacket;
import italy.mlsac.net.packet.ResetCause;
import italy.mlsac.net.packet.TeleportConfirmPacket;
import italy.mlsac.net.packet.ViewResetPacket;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.live.PacketFeed;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PacketIntegrityGuardTest {
    private static final long SECOND = 1_000_000_000L;

    private final UUID player = new UUID(7, 7);
    private final UUID other = new UUID(8, 8);
    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.parse("1.21.4").orElseThrow());
    private final SynchronousEventBus bus = new SynchronousEventBus(
            TestLogs.capturing(new CapturingLogSink(), new ManualClock(0), false));
    private final List<BadPacketObserved> reported = new ArrayList<>();
    private final List<String> pipelineCalls = new ArrayList<>();
    private final Set<UUID> exempt = new HashSet<>();
    private final Set<UUID> bedrock = new HashSet<>();
    private final Set<UUID> excludedZone = new HashSet<>();
    private final long[] now = {100 * SECOND};
    private BadPacketSettings settings = BadPacketSettings.pluginDefaults();
    private final PacketIntegrityGuard guard = new PacketIntegrityGuard(packets, bus, () -> settings, standing(),
            ClientTickPace.VANILLA, () -> now[0]);
    private final PacketFeed feed = new PacketFeed(packets, pipeline(), bus);

    @BeforeEach
    void start() {
        bus.subscribe(BadPacketObserved.class, reported::add);
        guard.start();
        feed.start();
    }

    @AfterEach
    void stop() {
        feed.stop();
        guard.stop();
    }

    @Test
    void realConfirmPassesUntouchedAndReachesThePipeline() {
        joinedLongAgo();
        packets.send(new PlayerPositionPacket(player, 2));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 2)));
        assertEquals(List.of("teleportConfirmed"), pipelineCalls);
        assertTrue(reported.isEmpty());
    }

    @Test
    void inventedIncrementingConfirmsAreDroppedAndReported() {
        joinedLongAgo();
        for (int id = 1_000_000; id < 1_000_005; id++) {
            assertTrue(packets.receive(new TeleportConfirmPacket(player, id)));
        }
        assertTrue(pipelineCalls.isEmpty());
        assertEquals(5, reported.size());
        assertEquals(new BadPacketObserved(player, BadPacketKind.FORGED_TELEPORT_CONFIRM), reported.get(0));
    }

    @Test
    void lateRepeatedAndOlderConfirmsPassWithoutBeingCounted() {
        joinedLongAgo();
        packets.send(new PlayerPositionPacket(player, 5));
        packets.send(new PlayerPositionPacket(player, 6));
        now[0] += 120 * SECOND;
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 6)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 6)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 3)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void confirmsAreJudgedPerPlayer() {
        joinedLongAgo();
        packets.send(new ViewResetPacket(other, ResetCause.JOIN_GAME));
        packets.send(new PlayerPositionPacket(other, 1));
        now[0] += 10 * SECOND;
        packets.send(new PlayerPositionPacket(player, 40));
        assertTrue(packets.receive(new TeleportConfirmPacket(other, 40)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 40)));
        assertEquals(List.of(new BadPacketObserved(other, BadPacketKind.FORGED_TELEPORT_CONFIRM)), reported);
    }

    @Test
    void joiningPlayerGetsASettlingPeriodForConfirmsOfThePreviousServer() {
        packets.send(new ViewResetPacket(player, ResetCause.JOIN_GAME));
        packets.send(new PlayerPositionPacket(player, 1));
        now[0] += SECOND;
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 523)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1)));
        now[0] += 5 * SECOND;
        assertTrue(packets.receive(new TeleportConfirmPacket(player, 524)));
        assertEquals(1, reported.size());
    }

    @Test
    void respawnAndWorldChangeKeepEarlierTeleportsConfirmable() {
        joinedLongAgo();
        packets.send(new PlayerPositionPacket(player, 7));
        packets.send(new ViewResetPacket(player, ResetCause.RESPAWN));
        packets.send(new PlayerPositionPacket(player, 8));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 7)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 8)));
        now[0] += 30 * SECOND;
        packets.send(new ViewResetPacket(player, ResetCause.CONFIGURATION_START));
        packets.send(new PlayerPositionPacket(player, 9));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 9)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void playerOnlineBeforeTheGuardStartedCannotBeJudgedUntilATeleportIsSeen() {
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        packets.send(new PlayerPositionPacket(player, 31));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 12)));
        assertTrue(packets.receive(new TeleportConfirmPacket(player, 1_000_001)));
        assertEquals(1, reported.size());
    }

    @Test
    void negativeIdsOfPluginViewTurnsAreNeverJudged() {
        joinedLongAgo();
        packets.send(new PlayerPositionPacket(player, -1));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, -1)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, -77)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void viaGeneratedConfirmWhileThePositionIsStillBeingWrittenPasses() {
        joinedLongAgo();
        List<Boolean> dropped = new ArrayList<>();
        packets.onOutbound(PlayerPositionPacket.class, ListenerOrder.OBSERVE, context ->
                dropped.add(packets.receive(new TeleportConfirmPacket(context.packet().viewer(),
                        context.packet().teleportId()))));
        packets.send(new PlayerPositionPacket(player, 3));
        packets.send(new PlayerPositionPacket(player, 4));
        assertEquals(List.of(false, false), dropped);
        assertTrue(reported.isEmpty());
    }

    @Test
    void viaConfirmDeferredToTheNextClientMovePasses() {
        joinedLongAgo();
        packets.send(new PlayerPositionPacket(player, 11));
        packets.send(new PlayerPositionPacket(player, 12));
        now[0] += 2 * SECOND;
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 11)));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 12)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void bypassBedrockAndExcludedZonesAreNeitherDroppedNorCounted() {
        joinedLongAgo();
        exempt.add(player);
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        exempt.clear();
        bedrock.add(player);
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_001)));
        bedrock.clear();
        excludedZone.add(player);
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_002)));
        assertTrue(reported.isEmpty());
        assertTrue(pipelineCalls.isEmpty());
    }

    @Test
    void switchedOffGuardDropsNothing() {
        joinedLongAgo();
        settings = new BadPacketSettings(false, 3, 60, 10, 30, 200);
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void departureAndStopForgetTheHistory() {
        joinedLongAgo();
        bus.publish(new PlayerLeft(player, "Fighter", false));
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        joinedLongAgo();
        guard.stop();
        guard.start();
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void confirmCancelledByAnEarlierListenerIsLeftAlone() {
        packets.onInbound(TeleportConfirmPacket.class, ListenerOrder.EARLY, context -> context.cancel());
        guard.stop();
        guard.start();
        packets.send(new PlayerPositionPacket(player, 1));
        assertTrue(packets.receive(new TeleportConfirmPacket(player, 1_000_000)));
        assertTrue(reported.isEmpty());
    }

    @Test
    void cancelledServerTeleportDoesNotWidenTheIssuedRange() {
        joinedLongAgo();
        packets.onOutbound(PlayerPositionPacket.class, ListenerOrder.EARLY, context -> {
            if (context.packet().teleportId() > 100) {
                context.cancel();
            }
        });
        packets.send(new PlayerPositionPacket(player, 500));
        assertTrue(packets.receive(new TeleportConfirmPacket(player, 500)));
        assertEquals(1, reported.size());
    }

    private void joinedLongAgo() {
        packets.send(new ViewResetPacket(player, ResetCause.JOIN_GAME));
        packets.send(new PlayerPositionPacket(player, 1));
        now[0] += 10 * SECOND;
        assertFalse(packets.receive(new TeleportConfirmPacket(player, 1)));
        pipelineCalls.clear();
    }

    private PlayerStanding standing() {
        return new PlayerStanding() {
            @Override
            public boolean valid(UUID id) {
                return true;
            }

            @Override
            public boolean exempt(UUID id) {
                return exempt.contains(id);
            }

            @Override
            public boolean bedrock(UUID id) {
                return bedrock.contains(id);
            }

            @Override
            public boolean zoneExcluded(UUID id) {
                return excludedZone.contains(id);
            }

            @Override
            public boolean animating(UUID id) {
                return false;
            }
        };
    }

    private PipelineInput pipeline() {
        return (PipelineInput) Proxy.newProxyInstance(PipelineInput.class.getClassLoader(),
                new Class<?>[] {PipelineInput.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("viewReset")) {
                        pipelineCalls.add(method.getName());
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                });
    }
}
