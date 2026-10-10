// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.packet.MetadataEntry;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.packet.VirtualEntity;
import italy.mlsac.net.packet.VirtualEntityType;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.platform.ServerVersionRules;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class PacketEventsSender implements PacketSender {

    private static final RelativeFlag KEEP_POSITION = RelativeFlag.X.or(RelativeFlag.Y).or(RelativeFlag.Z)
            .or(RelativeFlag.YAW).or(RelativeFlag.PITCH)
            .or(RelativeFlag.DELTA_X).or(RelativeFlag.DELTA_Y).or(RelativeFlag.DELTA_Z);

    private static final GameVersion PING_PACKET_FROM = GameVersion.of(1, 17, 0);

    private final PacketEventsAPI<?> api;
    private final ServerVersionRules rules;
    private final ReachableChannels channels;
    private final MetadataEncoder metadata = new MetadataEncoder();

    PacketEventsSender(PacketEventsAPI<?> api, ServerVersionRules rules, ReachableChannels channels) {
        this.api = api;
        this.rules = rules;
        this.channels = channels;
    }

    @Override
    public void sendTransactionPing(UUID viewer, int id) {
        Object channel = channels.channelOrNull(viewer);
        if (channel == null) {
            return;
        }
        PacketWrapper<?> ping = rules.server().atLeast(PING_PACKET_FROM)
                ? new WrapperPlayServerPing(id)
                : new WrapperPlayServerWindowConfirmation(0, (short) id, false);
        channels.attempt("sending a transaction ping", () -> protocol().sendPacket(channel, ping));
    }

    @Override
    public void spawnVirtual(UUID viewer, VirtualEntity entity) {
        write(viewer, spawnPacket(entity));
    }

    @Override
    public void announceEntity(UUID viewer, VirtualEntity entity) {
        announce(viewer, spawnPacket(entity));
    }

    @Override
    public void announceMove(UUID viewer, int entityNumber, double dx, double dy, double dz, float yaw, float pitch,
                             boolean grounded) {
        announce(viewer, new WrapperPlayServerEntityRelativeMoveAndRotation(entityNumber, dx, dy, dz, yaw, pitch,
                grounded));
    }

    @Override
    public void announceRemoval(UUID viewer, int... entityNumbers) {
        if (rules.destroyCarriesSingleId()) {
            for (int id : entityNumbers) {
                announce(viewer, new WrapperPlayServerDestroyEntities(id));
            }
        } else {
            announce(viewer, new WrapperPlayServerDestroyEntities(entityNumbers.clone()));
        }
    }

    private PacketWrapper<?> spawnPacket(VirtualEntity entity) {
        Vector3d at = new Vector3d(entity.x(), entity.y(), entity.z());
        if (entity.type() == VirtualEntityType.ARMOR_STAND && rules.spawnLivingEntityPacket()) {
            return new WrapperPlayServerSpawnLivingEntity(entity.entityNumber(), entity.uuid(), EntityTypes.ARMOR_STAND,
                    at, 0f, 0f, 0f, new Vector3d(0, 0, 0), List.of());
        }
        return new WrapperPlayServerSpawnEntity(entity.entityNumber(), Optional.of(entity.uuid()),
                entity.type() == VirtualEntityType.TEXT_DISPLAY ? EntityTypes.TEXT_DISPLAY : EntityTypes.ARMOR_STAND,
                at, 0f, 0f, 0f, 0, Optional.of(new Vector3d(0, 0, 0)));
    }

    @Override
    public void updateVirtualMetadata(UUID viewer, int entityNumber, List<MetadataEntry> entries) {
        write(viewer, new WrapperPlayServerEntityMetadata(entityNumber, metadata.encode(entries)));
    }

    @Override
    public void setPassengers(UUID viewer, int mountNumber, int[] passengers) {
        write(viewer, new WrapperPlayServerSetPassengers(mountNumber, passengers.clone()));
    }

    @Override
    public void destroyVirtual(UUID viewer, int... entityNumbers) {
        if (rules.destroyCarriesSingleId()) {
            for (int id : entityNumbers) {
                write(viewer, new WrapperPlayServerDestroyEntities(id));
            }
        } else {
            write(viewer, new WrapperPlayServerDestroyEntities(entityNumbers.clone()));
        }
    }

    @Override
    public void moveVirtual(UUID viewer, int entityNumber, double dx, double dy, double dz) {
        write(viewer, new WrapperPlayServerEntityRelativeMove(entityNumber, dx, dy, dz, false));
    }

    @Override
    public void teleportVirtual(UUID viewer, int entityNumber, double x, double y, double z) {
        write(viewer, new WrapperPlayServerEntityTeleport(entityNumber, new Vector3d(x, y, z), 0f, 0f, false));
    }

    @Override
    public void rotateView(UUID player, float yawDelta, float pitchDelta, int warpSequence) {
        Object channel = channels.channelOrNull(player);
        if (channel != null) {
            channels.attempt("turning a player's view", () -> protocol().sendPacket(channel,
                    new WrapperPlayServerPlayerPositionAndLook(warpSequence, new Vector3d(0, 0, 0),
                            new Vector3d(0, 0, 0), yawDelta, pitchDelta, KEEP_POSITION)));
        }
    }

    @Override
    public void replay(HeldPacket held) {
        if (held.payload() instanceof HeldBuffer buffer && held.settle()) {
            if (channels.carries(buffer.channel())) {
                channels.attempt("releasing a held packet",
                        () -> protocol().receivePacketSilently(buffer.channel(), buffer.buffer()));
            } else {
                ByteBufHelper.release(buffer.buffer());
            }
        }
    }

    @Override
    public void discard(HeldPacket held) {
        if (held.payload() instanceof HeldBuffer buffer && held.settle()) {
            ByteBufHelper.release(buffer.buffer());
        }
    }

    @Override
    public boolean onNetworkThread(UUID player, Runnable task) {
        return channels.runOnLoop(player, task);
    }

    @Override
    public boolean reachable(UUID player) {
        return channels.channelOrNull(player) != null;
    }

    @Override
    public void flush(UUID viewer) {
        Object channel = channels.channelOrNull(viewer);
        if (channel != null) {
            channels.attempt("flushing packets", () -> {
                User user = protocol().getUser(channel);
                if (user != null) {
                    user.flushPackets();
                }
            });
        }
    }

    private void announce(UUID viewer, PacketWrapper<?> packet) {
        Object channel = channels.channelOrNull(viewer);
        if (channel != null) {
            channels.attempt("sending a virtual entity packet", () -> protocol().sendPacket(channel, packet));
        }
    }

    private void write(UUID viewer, PacketWrapper<?> packet) {
        Object channel = channels.channelOrNull(viewer);
        if (channel != null) {
            channels.attempt("writing a virtual entity packet",
                    () -> protocol().writePacketSilently(channel, packet));
        }
    }

    private ProtocolManager protocol() {
        return api.getProtocolManager();
    }
}
