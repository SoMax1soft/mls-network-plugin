// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.input;

import java.util.UUID;

public record MovementPacket(
        UUID player,
        long nanos,
        float yaw,
        float pitch,
        boolean carriesRotation,
        double x,
        double y,
        double z,
        boolean carriesPosition,
        boolean grounded,
        boolean cancelled) {

    public static MovementPacket noMoveTick(UUID player, long nanos, float yaw, float pitch, boolean grounded) {
        return new MovementPacket(player, nanos, yaw, pitch, false, 0.0, 0.0, 0.0, false, grounded, false);
    }

    public boolean finite() {
        boolean rotationFinite = !carriesRotation || (Float.isFinite(yaw) && Float.isFinite(pitch));
        boolean positionFinite = !carriesPosition || (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z));
        return rotationFinite && positionFinite;
    }
}
