// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.nametag;

import italy.mlsac.net.nametag.ClientStage;
import italy.mlsac.net.nametag.DestroyReaction;
import italy.mlsac.net.nametag.NametagDirector;
import italy.mlsac.net.nametag.NametagPacket;
import italy.mlsac.net.nametag.SuspectPose;
import italy.mlsac.net.nametag.ViewerSight;
import italy.mlsac.net.packet.EntityDestroyPacket;
import italy.mlsac.net.packet.EntityMetadataPacket;
import italy.mlsac.net.packet.EntityMovePacket;
import italy.mlsac.net.packet.EntitySpawnPacket;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.InboundListener;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.MoveKind;
import italy.mlsac.net.packet.MovementPacket;
import italy.mlsac.net.packet.OutboundContext;
import italy.mlsac.net.packet.OutboundListener;
import italy.mlsac.net.packet.OutboundPacket;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.PassengersPacket;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.packet.ResetCause;
import italy.mlsac.net.packet.ViewResetPacket;
import italy.mlsac.net.workload.WorkloadCounters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class NametagPacketFollower {

    private final Supplier<Optional<NametagDirector>> directors;
    private final NametagWire wire;
    private final WorkloadCounters counters;
    private final List<Registration> registrations = new ArrayList<>();

    public NametagPacketFollower(Supplier<Optional<NametagDirector>> directors, NametagWire wire) {
        this(directors, wire, new WorkloadCounters());
    }

    public NametagPacketFollower(Supplier<Optional<NametagDirector>> directors, NametagWire wire,
                                 WorkloadCounters counters) {
        this.directors = directors;
        this.wire = wire;
        this.counters = counters;
    }

    public synchronized void listen(PacketLayer packets) {
        registrations.add(packets.onOutbound(EntitySpawnPacket.class, ListenerOrder.OBSERVE, metered(this::spawned)));
        registrations.add(packets.onOutbound(EntityDestroyPacket.class, ListenerOrder.OBSERVE,
                metered(this::destroyed)));
        registrations.add(packets.onOutbound(ViewResetPacket.class, ListenerOrder.OBSERVE, metered(this::reset)));
        registrations.add(packets.onOutbound(EntityMovePacket.class, ListenerOrder.OBSERVE, metered(this::moved)));
        registrations.add(packets.onOutbound(EntityMetadataPacket.class, ListenerOrder.OBSERVE, metered(this::posed)));
        registrations.add(packets.onOutbound(PassengersPacket.class, ListenerOrder.OBSERVE, metered(this::mounted)));
        registrations.add(packets.onInbound(MovementPacket.class, ListenerOrder.OBSERVE, meteredInbound(this::walked)));
    }

    private InboundListener<MovementPacket> meteredInbound(InboundListener<MovementPacket> listener) {
        return context -> {
            long began = System.nanoTime();
            try {
                listener.on(context);
            } finally {
                counters.hologramWork(System.nanoTime() - began);
            }
        };
    }

    private <P extends OutboundPacket> OutboundListener<P> metered(OutboundListener<P> listener) {
        return context -> {
            long began = System.nanoTime();
            try {
                listener.on(context);
            } finally {
                counters.hologramWork(System.nanoTime() - began);
            }
        };
    }

    public synchronized void deafen() {
        registrations.forEach(Registration::unregister);
        registrations.clear();
    }

    void spawned(OutboundContext<EntitySpawnPacket> context) {
        EntitySpawnPacket packet = context.packet();
        if (context.cancelled()) {
            return;
        }
        directors.get().ifPresent(director -> director.sight(packet.viewer()).ifPresent(sight -> sendAfter(context,
                packet.viewer(), sight.spawned(packet.entityNumber(), packet.x(), packet.y(), packet.z(),
                        director.currentLook()))));
    }

    void destroyed(OutboundContext<EntityDestroyPacket> context) {
        EntityDestroyPacket packet = context.packet();
        ViewerSight sight = live(context);
        if (sight == null) {
            return;
        }
        int[] ids = packet.entityNumbers();
        DestroyReaction reaction = sight.destroyed(Arrays.stream(ids).boxed().toList());
        if (reaction.rewritesPacket()) {
            int[] extended = IntStream.concat(Arrays.stream(ids),
                    reaction.idsToAppend().stream().mapToInt(Integer::intValue)).toArray();
            context.replace(new EntityDestroyPacket(packet.viewer(), extended));
        }
        sendAfter(context, packet.viewer(), reaction.packetsAfter());
    }

    void reset(OutboundContext<ViewResetPacket> context) {
        ViewerSight sight = live(context);
        if (sight != null) {
            sight.viewReset(context.packet().cause() == ResetCause.CONFIGURATION_START ? ClientStage.CONFIGURING
                    : ClientStage.IN_WORLD);
        }
    }

    void moved(OutboundContext<EntityMovePacket> context) {
        EntityMovePacket packet = context.packet();
        MoveKind kind = packet.kind();
        if (!kind.carriesPosition()) {
            return;
        }
        ViewerSight sight = live(context);
        if (sight == null) {
            return;
        }
        sendAfter(context, packet.viewer(), kind.relative()
                ? sight.movedBy(packet.entityNumber(), packet.x(), packet.y(), packet.z())
                : sight.movedTo(packet.entityNumber(), packet.x(), packet.y(), packet.z()));
    }

    void posed(OutboundContext<EntityMetadataPacket> context) {
        EntityMetadataPacket packet = context.packet();
        if (packet.pose().isEmpty()) {
            return;
        }
        SuspectPose pose = SuspectPose.fromGamePose(packet.pose().get().name());
        ViewerSight sight = live(context);
        if (sight != null) {
            sendAfter(context, packet.viewer(), sight.posed(packet.entityNumber(), pose));
        }
    }

    void mounted(OutboundContext<PassengersPacket> context) {
        PassengersPacket packet = context.packet();
        ViewerSight sight = live(context);
        if (sight == null) {
            return;
        }
        sight.passengers(packet.mountNumber(), Arrays.stream(packet.passengers()).boxed().toList())
                .ifPresent(rewritten -> context.replace(new PassengersPacket(packet.viewer(), packet.mountNumber(),
                        rewritten.stream().mapToInt(Integer::intValue).toArray())));
    }

    void walked(InboundContext<MovementPacket> context) {
        MovementPacket packet = context.packet();
        if (!packet.carriesPosition() || packet.cancelledEarlier() || context.cancelled()) {
            return;
        }
        Optional<NametagDirector> director = directors.get();
        ViewerSight sight = director.isPresent() ? director.get().sightOrNull(packet.player()) : null;
        if (sight == null) {
            return;
        }
        List<NametagPacket> follow = sight.ownMoved(packet.x(), packet.y(), packet.z());
        if (!follow.isEmpty()) {
            wire.send(packet.player(), follow);
        }
    }

    private ViewerSight live(OutboundContext<? extends OutboundPacket> context) {
        if (context.cancelled()) {
            return null;
        }
        Optional<NametagDirector> director = directors.get();
        return director.isPresent() ? director.get().sightOrNull(context.packet().viewer()) : null;
    }

    private void sendAfter(OutboundContext<?> context, UUID viewer, List<NametagPacket> packets) {
        if (!packets.isEmpty()) {
            context.afterSend(() -> wire.send(viewer, packets));
        }
    }
}
