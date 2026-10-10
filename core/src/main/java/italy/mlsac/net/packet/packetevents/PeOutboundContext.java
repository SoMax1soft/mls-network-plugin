// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import italy.mlsac.net.packet.EntityDestroyPacket;
import italy.mlsac.net.packet.OutboundContext;
import italy.mlsac.net.packet.OutboundPacket;
import italy.mlsac.net.packet.PassengersPacket;

final class PeOutboundContext<P extends OutboundPacket> implements OutboundContext<P> {

    private final PacketSendEvent event;
    private P packet;

    PeOutboundContext(PacketSendEvent event, P packet) {
        this.event = event;
        this.packet = packet;
    }

    @Override
    public P packet() {
        return packet;
    }

    @Override
    public boolean cancelled() {
        return event.isCancelled();
    }

    @Override
    public void cancel() {
        event.setCancelled(true);
    }

    @Override
    public void replace(P rewritten) {
        if (rewritten instanceof EntityDestroyPacket destroy) {
            WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(event);
            wrapper.setEntityIds(destroy.entityNumbers());
        } else if (rewritten instanceof PassengersPacket passengers) {
            WrapperPlayServerSetPassengers wrapper = new WrapperPlayServerSetPassengers(event);
            wrapper.setPassengers(passengers.passengers());
        } else {
            throw new IllegalArgumentException(rewritten.getClass().getSimpleName() + " cannot be rewritten");
        }
        event.markForReEncode(true);
        this.packet = rewritten;
    }

    @Override
    public void afterSend(Runnable action) {
        event.getPostTasks().add(action);
    }
}
