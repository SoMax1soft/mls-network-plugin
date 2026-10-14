// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.*;

import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.packet.*;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PacketTrustTest {
    private final UUID player = new UUID(1, 2);
    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.parse("1.21.4").orElseThrow());
    private final List<String> calls = new ArrayList<>();
    private final PipelineInput input = (PipelineInput) Proxy.newProxyInstance(PipelineInput.class.getClassLoader(),
            new Class<?>[] {PipelineInput.class}, (proxy, method, args) -> {
                calls.add(method.getName());
                return method.getReturnType() == boolean.class ? false : null;
            });

    @Test
    void inventedDuplicateAndCrossPlayerConfirmationsNeverReachThePipeline() {
        PacketFeed feed = new PacketFeed(packets, input);
        feed.start();
        packets.receive(new TeleportConfirmPacket(player, 1000000));
        assertTrue(calls.isEmpty());
        packets.send(new PlayerPositionPacket(player, 42));
        packets.receive(new TeleportConfirmPacket(new UUID(3, 4), 42));
        assertTrue(calls.isEmpty());
        packets.receive(new TeleportConfirmPacket(player, 42));
        packets.receive(new TeleportConfirmPacket(player, 42));
        assertEquals(List.of("teleportConfirmed"), calls);
        feed.stop();
    }

    @Test
    void cancelledServerPacketsCannotCreateReceiptsOrResetViews() {
        packets.onOutbound(PlayerPositionPacket.class, ListenerOrder.EARLY, context -> context.cancel());
        packets.onOutbound(TransactionPingPacket.class, ListenerOrder.EARLY, context -> context.cancel());
        packets.onOutbound(ViewResetPacket.class, ListenerOrder.EARLY, context -> context.cancel());
        PacketFeed feed = new PacketFeed(packets, input);
        feed.start();
        packets.send(new PlayerPositionPacket(player, 42));
        packets.send(new TransactionPingPacket(player, 42));
        packets.send(new ViewResetPacket(player, ResetCause.RESPAWN));
        packets.receive(new TeleportConfirmPacket(player, 42));
        assertTrue(calls.isEmpty());
        feed.stop();
    }

    @Test
    void cancelledConfirmationDoesNotConsumeAReceipt() {
        boolean[] cancel = {true};
        packets.onInbound(TeleportConfirmPacket.class, ListenerOrder.EARLY, context -> {
            if (cancel[0]) context.cancel();
        });
        PacketFeed feed = new PacketFeed(packets, input);
        feed.start();
        packets.send(new PlayerPositionPacket(player, -12));
        packets.receive(new TeleportConfirmPacket(player, -12));
        assertTrue(calls.isEmpty());
        cancel[0] = false;
        packets.receive(new TeleportConfirmPacket(player, -12));
        assertEquals(List.of("teleportConfirmed"), calls);
        feed.stop();
    }

    @Test
    void departureAndStopForgetOutstandingTeleports() {
        SynchronousEventBus bus = new SynchronousEventBus(TestLogs.capturing(new CapturingLogSink(), new ManualClock(0), false));
        PacketFeed feed = new PacketFeed(packets, input, bus);
        feed.start();
        packets.send(new PlayerPositionPacket(player, 42));
        bus.publish(new PlayerLeft(player, "Fighter", false));
        packets.receive(new TeleportConfirmPacket(player, 42));
        assertTrue(calls.isEmpty());
        packets.send(new PlayerPositionPacket(player, 43));
        feed.stop();
        feed.start();
        packets.receive(new TeleportConfirmPacket(player, 43));
        assertTrue(calls.isEmpty());
        feed.stop();
    }

    @Test
    void staleAndEvictedReceiptsAreRejected() {
        long[] now = {0};
        TeleportReceipts receipts = new TeleportReceipts(() -> now[0]);
        receipts.sent(player, 1);
        now[0] = 30_000_000_001L;
        assertFalse(receipts.accept(player, 1));
        for (int id = 0; id < 33; id++) receipts.sent(player, id);
        assertFalse(receipts.accept(player, 0));
        assertTrue(receipts.accept(player, 32));
        assertFalse(receipts.accept(player, 32));
    }

    @Test
    void cancelledPongCannotReleaseCompensation() {
        packets.onInbound(PongPacket.class, ListenerOrder.EARLY, context -> context.cancel());
        PacketFeed feed = new PacketFeed(packets, input);
        feed.start();
        packets.receive(new PongPacket(player, 42));
        assertTrue(calls.isEmpty());
        feed.stop();
    }

    @Test
    void cancelledTickEndAndInvalidSlotsCannotChangePipelineState() {
        packets.onInbound(ClientTickEndPacket.class, ListenerOrder.EARLY, context -> context.cancel());
        PacketFeed feed = new PacketFeed(packets, input);
        feed.start();
        packets.receive(new ClientTickEndPacket(player, 42));
        packets.receive(new HeldSlotPacket(player, -1));
        packets.receive(new HeldSlotPacket(player, 9));
        assertTrue(calls.isEmpty());
        packets.receive(new HeldSlotPacket(player, 8));
        assertEquals(List.of("slotChanged"), calls);
        feed.stop();
    }
}
