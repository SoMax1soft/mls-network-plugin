// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.HitMeasurement;

public record TargetMeasurement(LensReading lens, HitMeasurement hit) {
    public static final TargetMeasurement NONE = new TargetMeasurement(LensReading.NONE, HitMeasurement.INVALID);

    public boolean valid() {
        return lens.mode() != LensMode.NONE && hit.valid() && lens.tight() != null;
    }
}
