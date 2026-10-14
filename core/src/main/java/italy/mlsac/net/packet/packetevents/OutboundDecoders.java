// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import italy.mlsac.net.packet.EntityDestroyPacket;
import italy.mlsac.net.packet.EntityMetadataPacket;
import italy.mlsac.net.packet.EntityMovePacket;
import italy.mlsac.net.packet.EntitySpawnPacket;
import italy.mlsac.net.packet.MoveKind;
import italy.mlsac.net.packet.OutboundPacket;
import italy.mlsac.net.packet.PassengersPacket;
import italy.mlsac.net.packet.PlayerPositionPacket;
import italy.mlsac.net.packet.PoseKind;
import italy.mlsac.net.packet.ResetCause;
import italy.mlsac.net.packet.SpawnKind;
import italy.mlsac.net.packet.TransactionPingPacket;
import italy.mlsac.net.packet.ViewResetPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class OutboundDecoders {

    private static final int POSE_INDEX = 6;

    @FunctionalInterface
    interface Decoder {
        OutboundPacket decode(PacketSendEvent event, UUID viewer);
    }

    record Entry(Class<? extends OutboundPacket> produces, Decoder decoder) {
    }

    private final Map<PacketTypeCommon, Entry> entries = new HashMap<>();

    OutboundDecoders() {
        entries.put(PacketType.Play.Server.PLAYER_POSITION_AND_LOOK, new Entry(PlayerPositionPacket.class,
                (event, viewer) -> new PlayerPositionPacket(viewer,
                        new WrapperPlayServerPlayerPositionAndLook(event).getTeleportId())));
        entries.put(PacketType.Play.Server.PING, new Entry(TransactionPingPacket.class,
                (event, viewer) -> new TransactionPingPacket(viewer, new WrapperPlayServerPing(event).getId())));
        entries.put(PacketType.Play.Server.WINDOW_CONFIRMATION, new Entry(TransactionPingPacket.class,
                (event, viewer) -> new TransactionPingPacket(viewer,
                        new WrapperPlayServerWindowConfirmation(event).getActionId())));
        entries.put(PacketType.Play.Server.SPAWN_ENTITY, new Entry(EntitySpawnPacket.class, OutboundDecoders::spawnEntity));
        entries.put(PacketType.Play.Server.SPAWN_PLAYER, new Entry(EntitySpawnPacket.class, OutboundDecoders::spawnPlayer));
        entries.put(PacketType.Play.Server.SPAWN_LIVING_ENTITY, new Entry(EntitySpawnPacket.class,
                OutboundDecoders::spawnLiving));
        entries.put(PacketType.Play.Server.ENTITY_RELATIVE_MOVE, new Entry(EntityMovePacket.class, (event, viewer) -> {
            WrapperPlayServerEntityRelativeMove move = new WrapperPlayServerEntityRelativeMove(event);
            return new EntityMovePacket(viewer, move.getEntityId(), MoveKind.RELATIVE, move.getDeltaX(),
                    move.getDeltaY(), move.getDeltaZ(), move.isOnGround());
        }));
        entries.put(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION, new Entry(EntityMovePacket.class,
                (event, viewer) -> {
                    WrapperPlayServerEntityRelativeMoveAndRotation move =
                            new WrapperPlayServerEntityRelativeMoveAndRotation(event);
                    return new EntityMovePacket(viewer, move.getEntityId(), MoveKind.RELATIVE_WITH_ROTATION,
                            move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), move.isOnGround());
                }));
        entries.put(PacketType.Play.Server.ENTITY_ROTATION, new Entry(EntityMovePacket.class, (event, viewer) -> {
            WrapperPlayServerEntityRotation rotation = new WrapperPlayServerEntityRotation(event);
            return new EntityMovePacket(viewer, rotation.getEntityId(), MoveKind.ROTATION_ONLY, 0, 0, 0,
                    rotation.isOnGround());
        }));
        entries.put(PacketType.Play.Server.ENTITY_TELEPORT, new Entry(EntityMovePacket.class, (event, viewer) -> {
            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(event);
            Vector3d at = teleport.getPosition();
            return new EntityMovePacket(viewer, teleport.getEntityId(), MoveKind.TELEPORT, at.getX(), at.getY(),
                    at.getZ(), teleport.isOnGround());
        }));
        entries.put(PacketType.Play.Server.ENTITY_POSITION_SYNC, new Entry(EntityMovePacket.class, (event, viewer) -> {
            WrapperPlayServerEntityPositionSync sync = new WrapperPlayServerEntityPositionSync(event);
            Vector3d at = sync.getValues().getPosition();
            return new EntityMovePacket(viewer, sync.getId(), MoveKind.POSITION_SYNC, at.getX(), at.getY(), at.getZ(),
                    sync.isOnGround());
        }));
        entries.put(PacketType.Play.Server.DESTROY_ENTITIES, new Entry(EntityDestroyPacket.class,
                (event, viewer) -> new EntityDestroyPacket(viewer, new WrapperPlayServerDestroyEntities(event).getEntityIds())));
        entries.put(PacketType.Play.Server.RESPAWN, reset(ResetCause.RESPAWN));
        entries.put(PacketType.Play.Server.JOIN_GAME, reset(ResetCause.JOIN_GAME));
        entries.put(PacketType.Play.Server.CONFIGURATION_START, reset(ResetCause.CONFIGURATION_START));
        entries.put(PacketType.Play.Server.ENTITY_METADATA, new Entry(EntityMetadataPacket.class, OutboundDecoders::metadata));
        entries.put(PacketType.Play.Server.SET_PASSENGERS, new Entry(PassengersPacket.class, (event, viewer) -> {
            WrapperPlayServerSetPassengers passengers = new WrapperPlayServerSetPassengers(event);
            return new PassengersPacket(viewer, passengers.getEntityId(), passengers.getPassengers());
        }));
    }

    Optional<Entry> entryFor(PacketTypeCommon type) {
        return Optional.ofNullable(entries.get(type));
    }

    private static Entry reset(ResetCause cause) {
        return new Entry(ViewResetPacket.class, (event, viewer) -> new ViewResetPacket(viewer, cause));
    }

    private static OutboundPacket spawnEntity(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
        Vector3d at = spawn.getPosition();
        return new EntitySpawnPacket(viewer, spawn.getEntityId(), spawn.getUUID(), SpawnKind.GENERIC,
                spawn.getEntityType() == EntityTypes.PLAYER, at.getX(), at.getY(), at.getZ());
    }

    private static OutboundPacket spawnPlayer(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerSpawnPlayer spawn = new WrapperPlayServerSpawnPlayer(event);
        Vector3d at = spawn.getPosition();
        return new EntitySpawnPacket(viewer, spawn.getEntityId(), Optional.ofNullable(spawn.getUUID()),
                SpawnKind.PLAYER, true, at.getX(), at.getY(), at.getZ());
    }

    private static OutboundPacket spawnLiving(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerSpawnLivingEntity spawn = new WrapperPlayServerSpawnLivingEntity(event);
        Vector3d at = spawn.getPosition();
        return new EntitySpawnPacket(viewer, spawn.getEntityId(), Optional.ofNullable(spawn.getEntityUUID()),
                SpawnKind.LIVING, spawn.getEntityType() == EntityTypes.PLAYER, at.getX(), at.getY(), at.getZ());
    }

    private static OutboundPacket metadata(PacketSendEvent event, UUID viewer) {
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event);
        Optional<PoseKind> pose = Optional.empty();
        for (EntityData<?> entry : metadata.getEntityMetadata()) {
            if (entry.getIndex() == POSE_INDEX && entry.getValue() instanceof EntityPose value) {
                pose = Optional.of(poseOf(value));
            }
        }
        return new EntityMetadataPacket(viewer, metadata.getEntityId(), pose);
    }

    private static PoseKind poseOf(EntityPose pose) {
        return switch (pose) {
            case STANDING -> PoseKind.STANDING;
            case CROUCHING -> PoseKind.CROUCHING;
            case SWIMMING -> PoseKind.SWIMMING;
            case FALL_FLYING -> PoseKind.FALL_FLYING;
            case SPIN_ATTACK -> PoseKind.SPIN_ATTACK;
            case SLEEPING -> PoseKind.SLEEPING;
            default -> PoseKind.OTHER;
        };
    }
}
