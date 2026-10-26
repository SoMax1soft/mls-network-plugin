// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.banimation.ActorGesture;
import italy.mlsac.net.banimation.ActorKind;
import italy.mlsac.net.banimation.ActorPose;
import italy.mlsac.net.banimation.Placement;
import italy.mlsac.net.banimation.Point;
import italy.mlsac.net.packet.MetadataEntry;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.packet.VirtualEntity;
import italy.mlsac.net.packet.VirtualEntityType;
import italy.mlsac.net.packet.VirtualPose;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakeActors {

    static final int FIRST_ID = 1_500_000_000;
    static final int POSE_INDEX = 6;
    static final byte ATTACK_EVENT = 4;
    static final byte TENDRILS_EVENT = 61;
    static final byte SONIC_BOOM_EVENT = 62;

    private final PacketSender sender;
    private final AtomicInteger next = new AtomicInteger(FIRST_ID);
    private final Map<UUID, Actor> actors = new ConcurrentHashMap<>();

    public FakeActors(PacketSender sender) {
        this.sender = sender;
    }

    public void spawn(UUID victim, ActorKind kind, Placement at, ActorPose pose, List<UUID> viewers) {
        remove(victim);
        Actor actor = new Actor(next.getAndDecrement(), UUID.randomUUID(), List.copyOf(viewers), at);
        actors.put(victim, actor);
        Point point = at.point();
        VirtualEntity entity = new VirtualEntity(actor.id, actor.uuid, type(kind), point.x(), point.y(), point.z());
        for (UUID viewer : actor.viewers) {
            sender.spawnVirtualFacing(viewer, entity, at.yaw());
            sender.updateVirtualMetadata(viewer, actor.id, List.of(pose(pose)));
            sender.flush(viewer);
        }
    }

    public void pose(UUID victim, ActorPose pose) {
        Actor actor = actors.get(victim);
        if (actor == null) {
            return;
        }
        for (UUID viewer : actor.viewers) {
            sender.updateVirtualMetadata(viewer, actor.id, List.of(pose(pose)));
            sender.flush(viewer);
        }
    }

    public void move(UUID victim, Placement to) {
        Actor actor = actors.get(victim);
        if (actor == null) {
            return;
        }
        Point from = actor.at.point();
        Point target = new Point(to.point().x(), from.y(), to.point().z());
        actor.at = new Placement(target, to.yaw(), to.pitch());
        for (UUID viewer : actor.viewers) {
            sender.moveVirtualFacing(viewer, actor.id, target.x() - from.x(), 0, target.z() - from.z(), to.yaw());
            sender.flush(viewer);
        }
    }

    public void gesture(UUID victim, ActorGesture gesture) {
        Actor actor = actors.get(victim);
        if (actor == null) {
            return;
        }
        byte event = switch (gesture) {
            case ATTACK -> ATTACK_EVENT;
            case SHAKE_TENDRILS -> TENDRILS_EVENT;
            case SONIC_BOOM -> SONIC_BOOM_EVENT;
        };
        for (UUID viewer : actor.viewers) {
            sender.playVirtualEvent(viewer, actor.id, event);
            sender.flush(viewer);
        }
    }

    public void remove(UUID victim) {
        Actor actor = actors.remove(victim);
        if (actor == null) {
            return;
        }
        for (UUID viewer : actor.viewers) {
            sender.destroyVirtual(viewer, actor.id);
            sender.flush(viewer);
        }
    }

    private static MetadataEntry pose(ActorPose pose) {
        return new MetadataEntry.PoseValue(POSE_INDEX, VirtualPose.valueOf(pose.name()));
    }

    private static VirtualEntityType type(ActorKind kind) {
        return switch (kind) {
            case WARDEN -> VirtualEntityType.WARDEN;
        };
    }

    private static final class Actor {

        private final int id;
        private final UUID uuid;
        private final List<UUID> viewers;
        private volatile Placement at;

        private Actor(int id, UUID uuid, List<UUID> viewers, Placement at) {
            this.id = id;
            this.uuid = uuid;
            this.viewers = viewers;
            this.at = at;
        }
    }
}
