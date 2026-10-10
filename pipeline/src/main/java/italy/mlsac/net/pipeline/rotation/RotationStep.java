// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

public record RotationStep(
        boolean present,
        boolean valid,
        double yawTurn,
        double pitchTurn,
        double yawTurnChange,
        double pitchTurnChange,
        double yawTurnJolt,
        double pitchTurnJolt,
        double yawSensStep,
        double pitchSensStep,
        boolean yawSensKnown,
        boolean pitchSensKnown,
        double yawStepsTaken,
        double pitchStepsTaken,
        double yawDivisorResidue,
        double pitchDivisorResidue) {

    public static final RotationStep ABSENT =
            new RotationStep(false, false, 0, 0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 0, 0);
}
