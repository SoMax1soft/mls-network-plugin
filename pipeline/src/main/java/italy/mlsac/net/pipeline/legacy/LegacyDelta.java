// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

public record LegacyDelta(
        float yawTurn,
        float pitchTurn,
        float yawTurnChange,
        float pitchTurnChange,
        float yawTurnJolt,
        float pitchTurnJolt,
        float yawDivisorResidue,
        float pitchDivisorResidue) {

    public static final LegacyDelta ZERO = new LegacyDelta(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);

    public float[] values() {
        return new float[] {yawTurn, pitchTurn, yawTurnChange, pitchTurnChange, yawTurnJolt, pitchTurnJolt, yawDivisorResidue,
            pitchDivisorResidue};
    }

    public float[] transmitted() {
        return new float[] {yawTurn, pitchTurn, yawTurnChange, pitchTurnChange, yawTurnJolt, pitchTurnJolt};
    }
}
