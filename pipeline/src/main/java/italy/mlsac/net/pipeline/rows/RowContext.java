// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.geometry.LookPair;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.rotation.RotationStep;

record RowContext(
        long rowOrdinal,
        long segmentOrdinal,
        long gameTick,
        long now,
        int rotationSource,
        PlayerSample eye,
        PlayerSample previousEye,
        PlayerSample crosshairVictim,
        TargetMeasurement measurement,
        AimRelation aim,
        OnTarget onTarget,
        RotationStep step,
        LookPair look,
        float yaw,
        float pitch) {
}
