// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import italy.mlsac.net.console.DebugTally;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import italy.mlsac.net.packet.InboundListener;
import italy.mlsac.net.packet.InboundPacket;
import italy.mlsac.net.packet.OutboundListener;
import italy.mlsac.net.packet.OutboundPacket;
import italy.mlsac.net.packet.Registration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

final class OrderedDispatch implements PacketListener {

    private final InboundDecoders inboundDecoders;
    private final OutboundDecoders outboundDecoders;
    private final PacketFaultLog faults;
    private final LongSupplier receiveClock;
    private final Map<Class<?>, List<InboundListener<InboundPacket>>> inbound = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<OutboundListener<OutboundPacket>>> outbound = new ConcurrentHashMap<>();

    OrderedDispatch(InboundDecoders inboundDecoders, OutboundDecoders outboundDecoders, PacketFaultLog faults,
                    LongSupplier receiveClock) {
        this.inboundDecoders = inboundDecoders;
        this.outboundDecoders = outboundDecoders;
        this.faults = faults;
        this.receiveClock = receiveClock;
    }

    @SuppressWarnings("unchecked")
    <P extends InboundPacket> Registration addInbound(Class<P> type, InboundListener<P> listener) {
        InboundListener<InboundPacket> erased = (InboundListener<InboundPacket>) (InboundListener<?>) listener;
        List<InboundListener<InboundPacket>> list = inbound.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(erased);
        return () -> list.remove(erased);
    }

    @SuppressWarnings("unchecked")
    <P extends OutboundPacket> Registration addOutbound(Class<P> type, OutboundListener<P> listener) {
        OutboundListener<OutboundPacket> erased = (OutboundListener<OutboundPacket>) (OutboundListener<?>) listener;
        List<OutboundListener<OutboundPacket>> list = outbound.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(erased);
        return () -> list.remove(erased);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        List<InboundDecoders.Entry> entries = inboundDecoders.entriesFor(event.getPacketType());
        if (entries.isEmpty()) {
            return;
        }
        UUID player = playerOf(event.getUser());
        if (player == null) {
            DebugTally.count("drop.packet-without-player");
            return;
        }
        long receivedNanos = receiveClock.getAsLong();
        for (InboundDecoders.Entry entry : entries) {
            List<InboundListener<InboundPacket>> listeners = inbound.get(entry.produces());
            if (listeners != null && !listeners.isEmpty()) {
                deliver(event, player, receivedNanos, entry, listeners);
            }
        }
    }

    private void deliver(PacketReceiveEvent event, UUID player, long receivedNanos, InboundDecoders.Entry entry,
                         List<InboundListener<InboundPacket>> listeners) {
        Object buffer = event.getByteBuf();
        int readerIndex = ByteBufHelper.readerIndex(buffer);
        try {
            Optional<InboundPacket> packet = entry.decoder().decode(event, player, receivedNanos);
            ByteBufHelper.readerIndex(buffer, readerIndex);
            if (packet.isPresent()) {
                PeInboundContext<InboundPacket> context = new PeInboundContext<>(event, packet.get());
                for (InboundListener<InboundPacket> listener : listeners) {
                    listener.on(context);
                }
            }
        } catch (RuntimeException fault) {
            ByteBufHelper.readerIndex(buffer, readerIndex);
            faults.record(String.valueOf(event.getPacketType()), fault);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Optional<OutboundDecoders.Entry> entry = outboundDecoders.entryFor(event.getPacketType());
        if (entry.isEmpty()) {
            return;
        }
        List<OutboundListener<OutboundPacket>> listeners = outbound.get(entry.get().produces());
        UUID viewer = playerOf(event.getUser());
        if (listeners == null || listeners.isEmpty() || viewer == null) {
            return;
        }
        try {
            PeOutboundContext<OutboundPacket> context =
                    new PeOutboundContext<>(event, entry.get().decoder().decode(event, viewer));
            for (OutboundListener<OutboundPacket> listener : listeners) {
                listener.on(context);
            }
        } catch (RuntimeException fault) {
            faults.record(String.valueOf(event.getPacketType()), fault);
        }
    }

    private static UUID playerOf(User user) {
        return user == null ? null : user.getUUID();
    }
}
