// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;

public record LensReading(LensMode mode, double ping, int outstanding, Box possible, Box tight) {
    public static final LensReading NONE = new LensReading(LensMode.NONE, -1.0, 0, null, null);
}
