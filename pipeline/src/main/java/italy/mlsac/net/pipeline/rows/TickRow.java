// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.compensation.TargetMeasurement;
import italy.mlsac.net.pipeline.geometry.AimRelation;
import italy.mlsac.net.pipeline.occlusion.OcclusionCell;
import italy.mlsac.net.pipeline.rotation.RotationStep;

public record TickRow(
        RowTiming timing,
        PacketTraits packet,
        RotationStep rotation,
        MoveDelta move,
        InputCounts inputs,
        SelfState self,
        TargetView target,
        TargetMeasurement measurement,
        OcclusionCell occlusion,
        AimRelation aim,
        OnTarget onTarget) {
}
