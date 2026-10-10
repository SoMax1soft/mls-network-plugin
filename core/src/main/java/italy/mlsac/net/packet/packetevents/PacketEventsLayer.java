// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.packet.InboundListener;
import italy.mlsac.net.packet.InboundPacket;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.OutboundListener;
import italy.mlsac.net.packet.OutboundPacket;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.platform.ServerVersionRules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PacketEventsLayer implements PacketLayer, AutoCloseable {

    private final PacketEventsAPI<?> api;
    private final GameVersion serverVersion;
    private final Map<ListenerOrder, OrderedDispatch> dispatches = new EnumMap<>(ListenerOrder.class);
    private final List<PacketListenerCommon> registered = new ArrayList<>();
    private final PacketSender sender;
    private final ClientVersions clients;

    public PacketEventsLayer(PacketEventsRuntime runtime, GameVersion fallbackVersion, PluginLog log) {
        this.api = runtime.api();
        this.serverVersion = GameVersion.parse(api.getServerManager().getVersion().getReleaseName())
                .orElse(fallbackVersion);
        InboundDecoders inbound = new InboundDecoders();
        OutboundDecoders outbound = new OutboundDecoders();
        PacketFaultLog faults = new PacketFaultLog(log);
        for (ListenerOrder order : ListenerOrder.values()) {
            OrderedDispatch dispatch = new OrderedDispatch(inbound, outbound, faults, System::nanoTime);
            dispatches.put(order, dispatch);
            registered.add(api.getEventManager().registerListener(dispatch, priorityOf(order)));
        }
        this.sender = new PacketEventsSender(api, new ServerVersionRules(serverVersion),
                new ReachableChannels(new PacketEventsChannels(api), faults));
        this.clients = new PeClientVersions(api);
    }

    @Override
    public <P extends InboundPacket> Registration onInbound(Class<P> type, ListenerOrder order,
                                                             InboundListener<P> listener) {
        return dispatches.get(order).addInbound(type, listener);
    }

    @Override
    public <P extends OutboundPacket> Registration onOutbound(Class<P> type, ListenerOrder order,
                                                               OutboundListener<P> listener) {
        return dispatches.get(order).addOutbound(type, listener);
    }

    @Override
    public PacketSender sender() {
        return sender;
    }

    @Override
    public ClientVersions clients() {
        return clients;
    }

    @Override
    public GameVersion serverVersion() {
        return serverVersion;
    }

    @Override
    public void close() {
        for (PacketListenerCommon listener : registered) {
            api.getEventManager().unregisterListener(listener);
        }
        registered.clear();
    }

    private static PacketListenerPriority priorityOf(ListenerOrder order) {
        return switch (order) {
            case EARLY -> PacketListenerPriority.LOWEST;
            case NORMAL -> PacketListenerPriority.NORMAL;
            case LATE -> PacketListenerPriority.HIGHEST;
            case OBSERVE -> PacketListenerPriority.MONITOR;
        };
    }
}
