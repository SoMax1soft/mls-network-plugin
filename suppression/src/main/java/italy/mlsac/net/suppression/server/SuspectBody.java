// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import italy.mlsac.net.suppression.motion.Vec3;
import java.util.Optional;
import java.util.UUID;

public interface SuspectBody {

    UUID id();

    boolean dead();

    boolean creativeOrSpectator();

    boolean gliding();

    boolean mounted();

    Vec3 lookDirection();

    int storageSize();

    void permuteStorage(int[] sourceSlotForSlot);

    Optional<String> mainHandMaterial();

    void dropMainHand(Vec3 velocity, int pickupDelayTicks);

    void pushVelocity(Vec3 velocity);
}
