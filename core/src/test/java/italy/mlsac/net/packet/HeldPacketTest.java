// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.FakePacketLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HeldPacketTest {

    private static final UUID STEVE = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000000594a");

    @Test
    void heldPacketSettlesExactlyOnce() {
        HeldPacket held = new HeldPacket(STEVE, "attack");
        assertFalse(held.settled());
        assertTrue(held.settle());
        assertTrue(held.settled());
        assertFalse(held.settle());
    }

    @Test
    void replayAfterDiscardIsIgnoredAndTheOtherWayRound() {
        FakePacketLayer layer = new FakePacketLayer(GameVersion.of(1, 21, 4));
        HeldPacket first = new HeldPacket(STEVE, "first");
        HeldPacket second = new HeldPacket(STEVE, "second");
        layer.sender().discard(first);
        layer.sender().replay(first);
        layer.sender().replay(second);
        layer.sender().discard(second);
        assertEquals(List.of("discard first", "replay second"), layer.sent());
    }

    @Test
    void networkThreadTasksRunOnlyForConnectedPlayers() {
        FakePacketLayer layer = new FakePacketLayer(GameVersion.of(1, 21, 4));
        List<String> ran = new ArrayList<>();
        assertTrue(layer.sender().onNetworkThread(STEVE, () -> ran.add("connected")));
        layer.disconnect(STEVE);
        assertFalse(layer.sender().onNetworkThread(STEVE, () -> ran.add("gone")));
        assertEquals(List.of("connected"), ran);
        assertEquals(1, layer.networkTaskCount());
    }

    @Test
    void playerActionsReachTheirListenersAndCanBeCancelled() {
        FakePacketLayer layer = new FakePacketLayer(GameVersion.of(1, 21, 4));
        List<PlayerActionKind> seen = new ArrayList<>();
        layer.onInbound(PlayerActionPacket.class, ListenerOrder.NORMAL, context -> {
            seen.add(context.packet().kind());
            if (context.packet().kind() == PlayerActionKind.WINDOW_CLICK) {
                context.cancel();
            }
        });
        assertTrue(layer.receive(new PlayerActionPacket(STEVE, PlayerActionKind.WINDOW_CLICK)));
        assertFalse(layer.receive(new PlayerActionPacket(STEVE, PlayerActionKind.ABILITIES)));
        assertEquals(List.of(PlayerActionKind.WINDOW_CLICK, PlayerActionKind.ABILITIES), seen);
    }
}
