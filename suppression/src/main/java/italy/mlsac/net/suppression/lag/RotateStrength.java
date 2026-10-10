// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

public record RotateStrength(IntervalRange interval, double yawMin, double yawMax, double pitchSpan,
                             Pursuit pursuit) {
}
