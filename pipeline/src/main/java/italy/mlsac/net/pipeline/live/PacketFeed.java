// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.packet.AttackPacket;
import italy.mlsac.net.packet.ClientTickEndPacket;
import italy.mlsac.net.packet.EntityDestroyPacket;
import italy.mlsac.net.packet.EntityMovePacket;
import italy.mlsac.net.packet.EntitySpawnPacket;
import italy.mlsac.net.packet.HeldSlotPacket;
import italy.mlsac.net.packet.InboundContext;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.MovementPacket;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.packet.PongPacket;
import italy.mlsac.net.packet.PlayerPositionPacket;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.packet.SwingPacket;
import italy.mlsac.net.packet.TeleportConfirmPacket;
import italy.mlsac.net.packet.TransactionPingPacket;
import italy.mlsac.net.packet.ViewResetPacket;
import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import java.util.ArrayList;
import java.util.List;

public final class PacketFeed {

    private final PacketLayer packets;
    private final PipelineInput pipeline;
    private final List<Registration> registrations = new ArrayList<>();
    private final TeleportReceipts teleports = new TeleportReceipts(System::nanoTime);
    private final EventBus bus;
    private Subscription departure;

    public PacketFeed(PacketLayer packets, PipelineInput pipeline) {
        this(packets, pipeline, null);
    }

    public PacketFeed(PacketLayer packets, PipelineInput pipeline, EventBus bus) {
        this.packets = packets;
        this.pipeline = pipeline;
        this.bus = bus;
    }

    public void start() {
        if (bus != null) {
            departure = bus.subscribe(PlayerLeft.class, left -> teleports.forget(left.player()));
        }
        registrations.add(packets.onInbound(MovementPacket.class, ListenerOrder.LATE, this::movement));
        registrations.add(packets.onInbound(ClientTickEndPacket.class, ListenerOrder.LATE, this::clientTickEnded));
        registrations.add(packets.onInbound(TeleportConfirmPacket.class, ListenerOrder.LATE,
                context -> {
                    TeleportConfirmPacket packet = context.packet();
                    if (!context.cancelled() && teleports.accept(packet.player(), packet.warpSequence())) {
                        pipeline.teleportConfirmed(packet.player());
                    }
                }));
        registrations.add(packets.onOutbound(PlayerPositionPacket.class, ListenerOrder.OBSERVE, context -> {
            if (!context.cancelled()) {
                PlayerPositionPacket packet = context.packet();
                context.afterSend(() -> teleports.sent(packet.viewer(), packet.teleportId()));
            }
        }));
        registrations.add(packets.onInbound(SwingPacket.class, ListenerOrder.LATE, this::swing));
        registrations.add(packets.onInbound(HeldSlotPacket.class, ListenerOrder.LATE,
                context -> {
                    int slot = context.packet().slot();
                    if (!context.cancelled() && slot >= 0 && slot <= 8) {
                        pipeline.slotChanged(context.packet().player());
                    }
                }));
        registrations.add(packets.onInbound(AttackPacket.class, ListenerOrder.LATE, this::attack));
        registrations.add(packets.onInbound(PongPacket.class, ListenerOrder.LATE, this::pong));
        registrations.add(packets.onOutbound(TransactionPingPacket.class, ListenerOrder.OBSERVE,
                context -> {
                    if (!context.cancelled()) {
                        context.afterSend(() -> pipeline.pingWritten(context.packet().viewer(), context.packet().id()));
                    }
                }));
        registrations.add(packets.onOutbound(EntitySpawnPacket.class, ListenerOrder.OBSERVE, context -> {
            EntitySpawnPacket spawn = context.packet();
            if (spawn.playerEntity() && !context.cancelled()) {
                pipeline.entitySpawned(spawn.viewer(), spawn.entityNumber(), new Vec3(spawn.x(), spawn.y(), spawn.z()));
            }
        }));
        registrations.add(packets.onOutbound(EntityMovePacket.class, ListenerOrder.OBSERVE, context -> {
            EntityMovePacket move = context.packet();
            if (!context.cancelled()) {
                pipeline.entityMoved(move.viewer(), new EntityMove(move.entityNumber(), move.kind().relative(),
                        move.kind().carriesPosition(), move.x(), move.y(), move.z()));
            }
        }));
        registrations.add(packets.onOutbound(EntityDestroyPacket.class, ListenerOrder.OBSERVE, context -> {
            if (!context.cancelled()) {
                pipeline.entitiesDestroyed(context.packet().viewer(), context.packet().entityNumbers());
            }
        }));
        registrations.add(packets.onOutbound(ViewResetPacket.class, ListenerOrder.OBSERVE,
                context -> {
                    if (!context.cancelled()) {
                        teleports.forget(context.packet().viewer());
                        pipeline.viewReset(context.packet().viewer());
                    }
                }));
    }

    public void stop() {
        registrations.forEach(Registration::unregister);
        registrations.clear();
        if (departure != null) {
            departure.cancel();
            departure = null;
        }
        teleports.clear();
    }

    private void movement(InboundContext<MovementPacket> context) {
        MovementPacket packet = context.packet();
        pipeline.movement(new italy.mlsac.net.pipeline.input.MovementPacket(packet.player(), packet.receivedNanos(),
                packet.yaw(), packet.pitch(), packet.carriesRotation(), packet.x(), packet.y(), packet.z(),
                packet.carriesPosition(), packet.grounded(), context.cancelled()));
    }

    private void clientTickEnded(InboundContext<ClientTickEndPacket> context) {
        if (context.cancelled()) {
            return;
        }
        ClientTickEndPacket packet = context.packet();
        String version = packets.clients().versionName(packet.player()).orElse(null);
        pipeline.clientTickEnd(new TickEndPacket(packet.player(), packet.receivedNanos(), version));
    }

    private void swing(InboundContext<SwingPacket> context) {
        SwingPacket packet = context.packet();
        pipeline.swing(new italy.mlsac.net.pipeline.input.SwingPacket(packet.player(), packet.receivedNanos(),
                packet.mainHand()));
    }

    private void attack(InboundContext<AttackPacket> context) {
        AttackPacket packet = context.packet();
        pipeline.attack(new italy.mlsac.net.pipeline.input.AttackPacket(packet.player(), packet.receivedNanos(),
                packet.entityNumber(), context.cancelled()));
    }

    private void pong(InboundContext<PongPacket> context) {
        if (context.cancelled()) {
            return;
        }
        PongPacket packet = context.packet();
        if (pipeline.pong(packet.player(), packet.id())) {
            context.cancel();
        }
    }
}
