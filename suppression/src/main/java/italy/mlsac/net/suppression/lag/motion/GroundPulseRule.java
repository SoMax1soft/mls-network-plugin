// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.lag.GroundStrength;
import italy.mlsac.net.suppression.motion.Vec3;
import java.util.OptionalDouble;

public final class GroundPulseRule {

    private static final double JUMP_START_RISE = 0.2;

    public OptionalDouble pulseFor(GroundStep step, GroundStrength strength) {
        OptionalDouble candidate = candidate(step, strength);
        if (candidate.isPresent() && candidate.getAsDouble() < step.verticalDelta()) {
            return candidate;
        }
        return OptionalDouble.empty();
    }

    public Vec3 velocity(double vertical) {
        return new Vec3(0.0, vertical, 0.0);
    }

    private static OptionalDouble candidate(GroundStep step, GroundStrength strength) {
        if (step.wasOnGround() && !step.grounded() && step.verticalDelta() > JUMP_START_RISE) {
            return OptionalDouble.of(strength.jumpStartVertical());
        }
        int airborne = step.airborneTicks();
        if (!step.grounded() && airborne >= strength.firstAirTick()
                && (airborne - strength.firstAirTick()) % strength.airPeriodTicks() == 0) {
            return OptionalDouble.of(strength.airVertical());
        }
        return OptionalDouble.empty();
    }
}
