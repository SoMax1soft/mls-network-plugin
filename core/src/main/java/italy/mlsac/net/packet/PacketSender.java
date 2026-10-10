// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.List;
import java.util.UUID;

public interface PacketSender {

    void sendTransactionPing(UUID viewer, int id);

    void spawnVirtual(UUID viewer, VirtualEntity entity);

    void updateVirtualMetadata(UUID viewer, int entityNumber, List<MetadataEntry> entries);

    void setPassengers(UUID viewer, int mountNumber, int[] passengers);

    void destroyVirtual(UUID viewer, int... entityNumbers);

    void moveVirtual(UUID viewer, int entityNumber, double dx, double dy, double dz);

    void teleportVirtual(UUID viewer, int entityNumber, double x, double y, double z);

    void rotateView(UUID player, float yawDelta, float pitchDelta, int warpSequence);

    void replay(HeldPacket held);

    void discard(HeldPacket held);

    boolean onNetworkThread(UUID player, Runnable task);

    default boolean reachable(UUID player) {
        return true;
    }

    void flush(UUID viewer);

    default void announceEntity(UUID viewer, VirtualEntity entity) {
        spawnVirtual(viewer, entity);
    }

    default void announceMove(UUID viewer, int entityNumber, double dx, double dy, double dz, float yaw, float pitch,
                              boolean grounded) {
        moveVirtual(viewer, entityNumber, dx, dy, dz);
    }

    default void announceRemoval(UUID viewer, int... entityNumbers) {
        destroyVirtual(viewer, entityNumbers);
    }
}
