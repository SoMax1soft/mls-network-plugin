// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.banimation.AnimationDirector;
import italy.mlsac.net.banimation.AnimationEffects;
import italy.mlsac.net.banimation.AnimationLaunch;
import italy.mlsac.net.banimation.AnimationScript;
import italy.mlsac.net.banimation.Placement;
import italy.mlsac.net.banimation.Point;
import italy.mlsac.net.packet.AttackPacket;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.PlayerActionKind;
import italy.mlsac.net.packet.PlayerActionPacket;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.testkit.RecordingLog;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncomingActionShieldTest {

    private static final UUID SUSPECT = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000016fb1");
    private static final UUID BYSTANDER = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000018ea0");

    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.of(1, 21, 4));
    private final AnimationDirector director = new AnimationDirector(standingEffects(), () -> 0.5, new RecordingLog());
    private final IncomingActionShield shield = new IncomingActionShield(director);

    @Test
    void actionsOfAnAnimatingPlayerAreCancelledOthersPass() {
        shield.listen(packets);
        assertEquals(AnimationLaunch.STARTED, director.start(SUSPECT, stillScript(), "say done"));
        for (PlayerActionKind kind : PlayerActionKind.values()) {
            assertTrue(packets.receive(new PlayerActionPacket(SUSPECT, kind)), kind.name());
            assertFalse(packets.receive(new PlayerActionPacket(BYSTANDER, kind)), kind.name());
        }
    }

    @Test
    void laterListenersSeeTheCancelAndDeafenStopsTheShield() {
        List<Boolean> seenCancelled = new ArrayList<>();
        packets.onInbound(PlayerActionPacket.class, ListenerOrder.LATE,
                context -> seenCancelled.add(context.cancelled()));
        shield.listen(packets);
        director.start(SUSPECT, stillScript(), "say done");
        packets.receive(new PlayerActionPacket(SUSPECT, PlayerActionKind.INTERACT_ENTITY));
        shield.deafen();
        assertFalse(packets.receive(new PlayerActionPacket(SUSPECT, PlayerActionKind.INTERACT_ENTITY)));
        assertFalse(packets.receive(new AttackPacket(SUSPECT, 0L, 7, false)));
        assertEquals(List.of(true, false), seenCancelled);
    }

    private static AnimationScript stillScript() {
        return new AnimationScript("still", 20, false, false, false, 0, -1, Optional.empty(), List.of());
    }

    private static AnimationEffects standingEffects() {
        Placement origin = new Placement(new Point(0, 64, 0), 0f, 0f);
        return (AnimationEffects) Proxy.newProxyInstance(AnimationEffects.class.getClassLoader(),
                new Class<?>[]{AnimationEffects.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "online" -> true;
                    case "location" -> Optional.of(origin);
                    case "stripInventory" -> 0;
                    default -> null;
                });
    }
}
