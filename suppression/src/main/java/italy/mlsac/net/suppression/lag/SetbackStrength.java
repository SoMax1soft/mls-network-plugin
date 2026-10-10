// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

public record SetbackStrength(IntervalRange interval, int rewindTicks, SnapReach reach, Pursuit pursuit) {
}
