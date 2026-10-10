// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;

public record ClickRow(
        ClickTiming timing,
        ClickTarget target,
        TargetMeasurement measurement,
        OcclusionCell occlusion,
        AimRelation aim,
        OnTarget onTarget,
        AttackHistory history,
        AttackerState attacker,
        double eyeToBoxGap,
        float yaw,
        float pitch,
        double yawTurn,
        double pitchTurn) {
}
