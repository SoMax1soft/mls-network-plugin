// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import static italy.mlsac.net.suppression.server.bukkit.KitBench.STEVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.packet.AttackPacket;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.SwingPacket;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.server.bukkit.delay.HeldRelease;
import italy.mlsac.net.suppression.server.bukkit.delay.HitDelayGate;
import italy.mlsac.net.testkit.FakePacketLayer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HitDelayGateTest {

    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long HEAVY_HOLD = 500L;

    private final KitBench bench = new KitBench(LagLevel.HEAVY, LagKind.DELAY);
    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.of(1, 21, 4));
    private final ManualReleaseTimer timer = new ManualReleaseTimer();
    private final List<String> pipelineSaw = new ArrayList<>();

    @BeforeEach
    void wire() {
        HeldRelease release = new HeldRelease(bench.kit.delay(), bench.kit.lagRuntimes(), packets.sender(),
                bench.clock, timer);
        new HitDelayGate(packets, bench.kit.delay(), bench.kit.readout(), bench.kit.lagRuntimes(),
                bench.kit.combat(), bench.clock, release).register();
        packets.onInbound(AttackPacket.class, ListenerOrder.LATE,
                context -> pipelineSaw.add("attack cancelled=" + context.cancelled()));
        packets.onInbound(SwingPacket.class, ListenerOrder.LATE,
                context -> pipelineSaw.add("swing cancelled=" + context.cancelled()));
        bench.clock.set(1_000L);
    }

    private AttackPacket attackAt(long millis) {
        return new AttackPacket(STEVE, millis * NANOS_PER_MILLI, 42, false);
    }

    private SwingPacket swingAt(long millis) {
        return new SwingPacket(STEVE, millis * NANOS_PER_MILLI, true);
    }

    @Test
    void withoutDelayPacketsPassUntouched() {
        assertFalse(packets.receive(attackAt(1_000L)));
        assertEquals(List.of("attack cancelled=false"), pipelineSaw);
        assertEquals(List.of(), packets.sent());
        assertTrue(bench.kit.combat().engagedInFight(STEVE, 1_000L));
    }

    @Test
    void heldPacketsAreSeenOnceThenReplayedInOrderWhenDue() {
        bench.flag();
        assertTrue(packets.receive(swingAt(1_000L)));
        assertTrue(packets.receive(attackAt(1_010L)));
        assertEquals(List.of("swing cancelled=false", "attack cancelled=false"), pipelineSaw);
        assertEquals(List.of(), packets.sent());
        assertEquals(1, timer.pendingCount());

        bench.clock.set(1_000L + HEAVY_HOLD - 1);
        timer.runPending();
        assertEquals(List.of(), packets.sent());

        bench.clock.set(1_010L + HEAVY_HOLD);
        timer.runPending();
        assertEquals(List.of("replay " + swingAt(1_000L), "replay " + attackAt(1_010L)), packets.sent());
        assertEquals(0, timer.pendingCount());
        assertEquals(2, pipelineSaw.size());
    }

    @Test
    void packetsCancelledByOthersAreNeverHeld() {
        bench.flag();
        packets.onInbound(AttackPacket.class, ListenerOrder.NORMAL, context -> context.cancel());
        assertTrue(packets.receive(attackAt(1_000L)));
        assertEquals(List.of("attack cancelled=true"), pipelineSaw);
        assertFalse(bench.kit.lagRuntimes().existing(STEVE)
                .map(runtime -> runtime.heldPackets().hasQueued()).orElse(false));
        assertEquals(0, timer.pendingCount());
    }

    @Test
    void discardedQueueStopsTheReleaseChain() {
        bench.flag();
        packets.receive(attackAt(1_000L));
        bench.kit.lagRuntimes().died(STEVE);
        bench.clock.set(5_000L);
        timer.runPending();
        assertEquals(List.of(), packets.sent());
        assertEquals(0, timer.pendingCount());
    }

    @Test
    void dueReleaseRunsOnTheSuspectsNetworkThread() {
        bench.flag();
        packets.receive(attackAt(1_000L));
        bench.clock.set(1_000L + HEAVY_HOLD);
        timer.runPending();
        assertEquals(List.of("replay " + attackAt(1_000L)), packets.sent());
        assertEquals(1, packets.networkTaskCount());
    }

    @Test
    void dueReleaseOfADisconnectedSuspectFreesThePackets() {
        bench.flag();
        packets.receive(attackAt(1_000L));
        packets.disconnect(STEVE);
        bench.clock.set(1_000L + HEAVY_HOLD);
        timer.runPending();
        assertEquals(List.of("discard " + attackAt(1_000L)), packets.sent());
        assertEquals(0, timer.pendingCount());
    }

    @Test
    void recordedSuspectIsNotDelayed() {
        bench.server.startRecording(STEVE);
        bench.flag();
        assertFalse(packets.receive(swingAt(1_000L)));
        assertFalse(packets.receive(attackAt(1_010L)));
        assertEquals(List.of(), packets.sent());
        assertEquals(0, timer.pendingCount());
    }

    @Test
    void recordingStartedMidLagReleasesHeldPacketsInOrderBeforeTheNextOne() {
        bench.flag();
        assertTrue(packets.receive(swingAt(1_000L)));
        assertTrue(packets.receive(attackAt(1_010L)));
        bench.server.startRecording(STEVE);
        bench.clock.set(1_020L);
        assertTrue(packets.receive(swingAt(1_020L)));
        assertEquals(List.of("replay " + swingAt(1_000L), "replay " + attackAt(1_010L), "replay " + swingAt(1_020L)),
                packets.sent());
        assertEquals(3, pipelineSaw.size());
        assertFalse(packets.receive(attackAt(1_030L)));
        assertEquals(3, packets.sent().size());
        timer.runPending();
        assertEquals(3, packets.sent().size());
        assertEquals(0, timer.pendingCount());
    }

    @Test
    void recordingStartedMidLagReleasesEveryHeldPacketAtTheFirstRelease() {
        bench.flag();
        packets.receive(swingAt(1_000L));
        packets.receive(attackAt(1_010L));
        packets.receive(swingAt(1_200L));
        bench.server.startRecording(STEVE);
        bench.clock.set(1_000L + HEAVY_HOLD);
        timer.runPending();
        assertEquals(List.of("replay " + swingAt(1_000L), "replay " + attackAt(1_010L), "replay " + swingAt(1_200L)),
                packets.sent());
        assertEquals(0, timer.pendingCount());
    }

    @Test
    void delayResumesWhenTheRecordingStopsWhileTheLagIsActive() {
        bench.flag();
        bench.server.startRecording(STEVE);
        assertFalse(packets.receive(attackAt(1_000L)));
        bench.server.stopRecording(STEVE);
        assertTrue(packets.receive(attackAt(1_100L)));
        bench.clock.set(1_100L + HEAVY_HOLD);
        timer.runPending();
        assertEquals(List.of("replay " + attackAt(1_100L)), packets.sent());
    }

    @Test
    void exemptSuspectIsNotDelayed() {
        bench.server.grantBypass(STEVE);
        bench.flag();
        assertFalse(packets.receive(attackAt(1_000L)));
        assertEquals(List.of(), packets.sent());
    }
}
