// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.motion.WorldPoint;
import java.util.Optional;

public final class GroundState {

    private WorldPoint previous;
    private boolean previousOnGround = true;
    private int airborneTicks;

    public Optional<WorldPoint> previous() {
        return Optional.ofNullable(previous);
    }

    public boolean previousOnGround() {
        return previousOnGround;
    }

    public int advanceAirborne(boolean grounded) {
        airborneTicks = grounded ? 0 : airborneTicks + 1;
        return airborneTicks;
    }

    public int airborneTicks() {
        return airborneTicks;
    }

    public void resetAirborne() {
        airborneTicks = 0;
    }

    public void remember(WorldPoint position, boolean grounded) {
        previous = position;
        previousOnGround = grounded;
    }

    public void reset() {
        previous = null;
        previousOnGround = true;
        airborneTicks = 0;
    }
}
