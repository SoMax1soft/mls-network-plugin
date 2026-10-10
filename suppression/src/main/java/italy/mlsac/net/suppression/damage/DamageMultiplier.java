// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.damage;

public record DamageMultiplier(double value) {

    private static final double NEUTRAL_TOLERANCE = 0.0001;

    public static DamageMultiplier of(int dealtPercent, int takenPercent) {
        return new DamageMultiplier((1.0 - dealtPercent / 100.0) * (1.0 + takenPercent / 100.0));
    }

    public boolean neutral() {
        return Math.abs(value - 1.0) < NEUTRAL_TOLERANCE;
    }
}
