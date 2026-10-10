// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.geometry.Vec3;
import java.util.UUID;

public interface ViewerPacketIntake {
    void pingWritten(UUID viewer, int transactionId);

    void entitySpawned(UUID viewer, int entityNumber, Vec3 position);

    void entityMoved(UUID viewer, EntityMove move);

    void entitiesDestroyed(UUID viewer, int[] entityNumbers);

    void viewReset(UUID viewer);
}
