// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.motion.WorldPoint;
import italy.mlsac.net.suppression.motion.Vec3;

public interface LagActuator {

    void setBack(WorldPoint target, float fallDistanceToRestore);

    void pushVelocity(Vec3 velocity);

    void turnView(int syncId, double yawDelta, double pitchDelta);
}
