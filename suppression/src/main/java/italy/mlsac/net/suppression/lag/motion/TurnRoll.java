// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.RotateStrength;

public final class TurnRoll {

    private static final double PITCH_LIMIT = 90.0;

    private final EngineRandom random;

    public TurnRoll(EngineRandom random) {
        this.random = random;
    }

    public Turn roll(RotateStrength strength, double currentPitch) {
        double sign = random.nextUnit() < 0.5 ? 1.0 : -1.0;
        double yaw = sign * random.uniform(strength.yawMin(), strength.yawMax());
        double pitch = random.uniform(-strength.pitchSpan(), strength.pitchSpan());
        double target = Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, currentPitch + pitch));
        return new Turn(yaw, target - currentPitch);
    }
}
