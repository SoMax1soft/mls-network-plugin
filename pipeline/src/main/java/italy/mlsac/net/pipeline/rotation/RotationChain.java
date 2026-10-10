// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

import italy.mlsac.net.pipeline.numeric.AngleWrap;

public final class RotationChain {
    private final SensitivityEstimator estimator = new SensitivityEstimator();
    private boolean hasLast;
    private float analyzedYaw;
    private float analyzedPitch;
    private double yawTurnBefore;
    private double pitchTurnBefore;
    private double previousAccelYaw;
    private double previousAccelPitch;

    public RotationStep rotate(float yaw, float pitch) {
        if (!hasLast) {
            hasLast = true;
            analyzedYaw = yaw;
            analyzedPitch = pitch;
            return step(true, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double yawTurn = AngleWrap.REMAINDER.apply((double) yaw - (double) analyzedYaw);
        double pitchTurn = (double) pitch - (double) analyzedPitch;
        estimator.observe(Math.abs(yawTurn), Math.abs(pitchTurn), yaw, analyzedYaw);
        analyzedYaw = yaw;
        analyzedPitch = pitch;
        return advance(true, yawTurn, pitchTurn);
    }

    public RotationStep still() {
        if (!hasLast) {
            return RotationStep.ABSENT;
        }
        return advance(false, 0.0, 0.0);
    }

    public void severRotation() {
        hasLast = false;
        yawTurnBefore = 0.0;
        pitchTurnBefore = 0.0;
        previousAccelYaw = 0.0;
        previousAccelPitch = 0.0;
    }

    private RotationStep advance(boolean present, double yawTurn, double pitchTurn) {
        double yawTurnChange = yawTurn - yawTurnBefore;
        double pitchTurnChange = pitchTurn - pitchTurnBefore;
        double yawTurnJolt = yawTurnChange - previousAccelYaw;
        double pitchTurnJolt = pitchTurnChange - previousAccelPitch;
        yawTurnBefore = yawTurn;
        pitchTurnBefore = pitchTurn;
        previousAccelYaw = yawTurnChange;
        previousAccelPitch = pitchTurnChange;
        return step(present, true, yawTurn, pitchTurn, yawTurnChange, pitchTurnChange, yawTurnJolt, pitchTurnJolt);
    }

    private RotationStep step(boolean present, boolean valid, double yawTurn, double pitchTurn, double yawTurnChange,
            double pitchTurnChange, double yawTurnJolt, double pitchTurnJolt) {
        boolean yawValid = estimator.known(LookAxis.YAW);
        boolean pitchValid = estimator.known(LookAxis.PITCH);
        double yawSensStep = estimator.step(LookAxis.YAW);
        double pitchSensStep = estimator.step(LookAxis.PITCH);
        return new RotationStep(present, valid, yawTurn, pitchTurn, yawTurnChange, pitchTurnChange, yawTurnJolt, pitchTurnJolt,
                yawSensStep, pitchSensStep, yawValid, pitchValid,
                dots(yawTurn, yawSensStep, yawValid), dots(pitchTurn, pitchSensStep, pitchValid),
                divisorResidue(yawTurn, yawSensStep, yawValid), divisorResidue(pitchTurn, pitchSensStep, pitchValid));
    }

    private static double dots(double delta, double mode, boolean valid) {
        return valid && mode > 0 ? Math.abs(delta) / mode : 0.0;
    }

    private static double divisorResidue(double delta, double mode, boolean valid) {
        if (!valid) {
            return 0.0;
        }
        double rest = Math.abs(delta) % mode;
        return Math.min(rest, mode - rest);
    }
}
