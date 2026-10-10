// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.module;

import italy.mlsac.net.detection.DetectionStates;
import italy.mlsac.net.penalty.PenaltyCommands;
import italy.mlsac.net.staff.StaffChannel;
import java.util.Objects;

public record FeaturePorts(PenaltyCommands penalties, StaffChannel staff, DetectionStates detections) {

    public FeaturePorts {
        Objects.requireNonNull(penalties, "penalties");
        Objects.requireNonNull(staff, "staff");
        Objects.requireNonNull(detections, "detections");
    }
}
