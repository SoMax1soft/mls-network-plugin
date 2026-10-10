// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

public record GroundStep(boolean wasOnGround, boolean grounded, double verticalDelta, int airborneTicks) {
}
