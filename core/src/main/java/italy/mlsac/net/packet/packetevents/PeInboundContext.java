// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.InboundPacket;

final class PeInboundContext<P extends InboundPacket> implements InboundContext<P> {

    private final PacketReceiveEvent event;
    private final P packet;

    PeInboundContext(PacketReceiveEvent event, P packet) {
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
    public HeldPacket hold() {
        Object buffer = event.getFullBufferClone();
        event.setCancelled(true);
        return new HeldPacket(packet.player(), new HeldBuffer(event.getChannel(), buffer));
    }
}
