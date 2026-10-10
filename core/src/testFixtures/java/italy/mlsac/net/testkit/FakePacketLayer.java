// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.InboundListener;
import italy.mlsac.net.packet.InboundPacket;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.MetadataEntry;
import italy.mlsac.net.packet.OutboundContext;
import italy.mlsac.net.packet.OutboundListener;
import italy.mlsac.net.packet.OutboundPacket;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.packet.VirtualEntity;
import italy.mlsac.net.platform.GameVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakePacketLayer implements PacketLayer {

    private final Map<ListenerOrder, List<Typed<?>>> inbound = new EnumMap<>(ListenerOrder.class);
    private final Map<ListenerOrder, List<Typed<?>>> outbound = new EnumMap<>(ListenerOrder.class);
    private final Map<UUID, GameVersion> clientVersions = new HashMap<>();
    private final List<String> sent = new CopyOnWriteArrayList<>();
    private final Set<UUID> disconnected = ConcurrentHashMap.newKeySet();
    private final AtomicInteger networkTasks = new AtomicInteger();
    private final GameVersion serverVersion;

    public FakePacketLayer(GameVersion serverVersion) {
        this.serverVersion = serverVersion;
        for (ListenerOrder order : ListenerOrder.values()) {
            inbound.put(order, new CopyOnWriteArrayList<>());
            outbound.put(order, new CopyOnWriteArrayList<>());
        }
    }

    public void setClientVersion(UUID player, GameVersion version) {
        clientVersions.put(player, version);
    }

    public List<String> sent() {
        return List.copyOf(sent);
    }

    public void disconnect(UUID player) {
        disconnected.add(player);
    }

    public int networkTaskCount() {
        return networkTasks.get();
    }

    @Override
    public <P extends InboundPacket> Registration onInbound(Class<P> type, ListenerOrder order,
                                                             InboundListener<P> listener) {
        Typed<P> typed = new Typed<>(type, listener);
        inbound.get(order).add(typed);
        return () -> inbound.get(order).remove(typed);
    }

    @Override
    public <P extends OutboundPacket> Registration onOutbound(Class<P> type, ListenerOrder order,
                                                               OutboundListener<P> listener) {
        Typed<P> typed = new Typed<>(type, listener);
        outbound.get(order).add(typed);
        return () -> outbound.get(order).remove(typed);
    }

    @SuppressWarnings("unchecked")
    public <P extends InboundPacket> boolean receive(P packet) {
        boolean[] cancelled = {false};
        InboundContext<P> context = new InboundContext<>() {
            @Override
            public P packet() {
                return packet;
            }

            @Override
            public boolean cancelled() {
                return cancelled[0];
            }

            @Override
            public void cancel() {
                cancelled[0] = true;
            }

            @Override
            public HeldPacket hold() {
                cancelled[0] = true;
                return new HeldPacket(packet.player(), packet);
            }
        };
        for (ListenerOrder order : ListenerOrder.values()) {
            for (Typed<?> typed : inbound.get(order)) {
                if (typed.type().isInstance(packet)) {
                    ((InboundListener<P>) typed.listener()).on(context);
                }
            }
        }
        return cancelled[0];
    }

    @SuppressWarnings("unchecked")
    public <P extends OutboundPacket> P send(P packet) {
        List<P> current = new ArrayList<>(List.of(packet));
        boolean[] cancelled = {false};
        OutboundContext<P> context = new OutboundContext<>() {
            @Override
            public P packet() {
                return current.get(0);
            }

            @Override
            public boolean cancelled() {
                return cancelled[0];
            }

            @Override
            public void cancel() {
                cancelled[0] = true;
            }

            @Override
            public void replace(P rewritten) {
                current.set(0, rewritten);
            }

            @Override
            public void afterSend(Runnable action) {
                action.run();
            }
        };
        for (ListenerOrder order : ListenerOrder.values()) {
            for (Typed<?> typed : outbound.get(order)) {
                if (typed.type().isInstance(packet)) {
                    ((OutboundListener<P>) typed.listener()).on(context);
                }
            }
        }
        return current.get(0);
    }

    @Override
    public PacketSender sender() {
        return new PacketSender() {
            @Override
            public void sendTransactionPing(UUID viewer, int id) {
                sent.add("ping " + viewer + " " + id);
            }

            @Override
            public void spawnVirtual(UUID viewer, VirtualEntity entity) {
                sent.add("spawn " + viewer + " " + entity);
            }

            @Override
            public void updateVirtualMetadata(UUID viewer, int entityNumber, List<MetadataEntry> entries) {
                sent.add("metadata " + viewer + " " + entityNumber + " " + entries);
            }

            @Override
            public void setPassengers(UUID viewer, int mountNumber, int[] passengers) {
                sent.add("passengers " + viewer + " " + mountNumber + " " + Arrays.toString(passengers));
            }

            @Override
            public void destroyVirtual(UUID viewer, int... entityNumbers) {
                sent.add("destroy " + viewer + " " + Arrays.toString(entityNumbers));
            }

            @Override
            public void moveVirtual(UUID viewer, int entityNumber, double dx, double dy, double dz) {
                sent.add("move " + viewer + " " + entityNumber + " " + dx + " " + dy + " " + dz);
            }

            @Override
            public void teleportVirtual(UUID viewer, int entityNumber, double x, double y, double z) {
                sent.add("teleport " + viewer + " " + entityNumber + " " + x + " " + y + " " + z);
            }

            @Override
            public void rotateView(UUID player, float yawDelta, float pitchDelta, int warpSequence) {
                sent.add("rotate " + player + " " + yawDelta + " " + pitchDelta + " " + warpSequence);
            }

            @Override
            public void replay(HeldPacket held) {
                if (held.settle()) {
                    sent.add("replay " + held.payload());
                }
            }

            @Override
            public void discard(HeldPacket held) {
                if (held.settle()) {
                    sent.add("discard " + held.payload());
                }
            }

            @Override
            public boolean onNetworkThread(UUID player, Runnable task) {
                if (disconnected.contains(player)) {
                    return false;
                }
                networkTasks.incrementAndGet();
                task.run();
                return true;
            }

            @Override
            public void flush(UUID viewer) {
                sent.add("flush " + viewer);
            }
        };
    }

    @Override
    public ClientVersions clients() {
        return new ClientVersions() {
            @Override
            public Optional<GameVersion> version(UUID player) {
                return Optional.ofNullable(clientVersions.get(player));
            }

            @Override
            public Optional<String> versionName(UUID player) {
                return version(player).map(v -> "V_" + v.toString().replace('.', '_'));
            }

            @Override
            public OptionalInt protocol(UUID player) {
                return OptionalInt.empty();
            }
        };
    }

    @Override
    public GameVersion serverVersion() {
        return serverVersion;
    }

    private record Typed<P>(Class<P> type, Object listener) {
    }
}
