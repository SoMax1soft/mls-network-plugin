// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

public final class BaseDamageCorrection {

    static final double TOLERANCE = 0.01;
    static final double NEAR_ZERO = 1e-9;
    static final int MAX_PASSES = 2;

    private BaseDamageCorrection() {
    }

    public static boolean needsCorrection(double finalDamage, double targetFinal) {
        return Math.abs(finalDamage - targetFinal) > TOLERANCE;
    }

    public static double correctedBase(double base, double currentFinal, double targetFinal) {
        double corrected = Math.abs(currentFinal) < NEAR_ZERO ? base + targetFinal : base * targetFinal / currentFinal;
        return Math.max(0.0, corrected);
    }
}
