// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import italy.mlsac.net.pipeline.numeric.AngleWrap;

public final class LegacyRotationAnalyzer {
    private final LegacyAxis yawAxis = new LegacyAxis();
    private final LegacyAxis pitchAxis = new LegacyAxis();
    private boolean hasLast;
    private float analyzedYaw;
    private float analyzedPitch;

    public void reset() {
        yawAxis.reset();
        pitchAxis.reset();
        hasLast = false;
        analyzedYaw = 0f;
        analyzedPitch = 0f;
    }

    double yawStepEstimate() {
        return yawAxis.mode();
    }

    public LegacyDelta analyze(float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            return LegacyDelta.ZERO;
        }
        if (!hasLast) {
            analyzedYaw = yaw;
            analyzedPitch = pitch;
            yawAxis.restart();
            pitchAxis.restart();
            hasLast = true;
            return LegacyDelta.ZERO;
        }
        float yawTurn = AngleWrap.remainderSingle(yaw - analyzedYaw);
        float pitchTurn = pitch - analyzedPitch;
        float[] yawStep = yawAxis.step(yawTurn);
        float[] pitchStep = pitchAxis.step(pitchTurn);
        analyzedYaw = yaw;
        analyzedPitch = pitch;
        return new LegacyDelta(yawTurn, pitchTurn, yawStep[0], pitchStep[0], yawStep[1], pitchStep[1],
                yawStep[2], pitchStep[2]);
    }
}
