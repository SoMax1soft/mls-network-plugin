// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.banimation.AnimationDirector;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.PlayerActionPacket;
import italy.mlsac.net.packet.Registration;
import java.util.Optional;

public final class IncomingActionShield {

    private final AnimationDirector animations;
    private Optional<Registration> registration = Optional.empty();

    public IncomingActionShield(AnimationDirector animations) {
        this.animations = animations;
    }

    public synchronized void listen(PacketLayer packets) {
        if (registration.isEmpty()) {
            registration = Optional.of(packets.onInbound(PlayerActionPacket.class, ListenerOrder.NORMAL, this::seen));
        }
    }

    public synchronized void deafen() {
        registration.ifPresent(Registration::unregister);
        registration = Optional.empty();
    }

    void seen(InboundContext<PlayerActionPacket> context) {
        if (animations.animating(context.packet().player())) {
            context.cancel();
        }
    }
}
