// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.damage;

public final class FinalDamageScaler {

    public static final double TOLERANCE = 0.01;
    private static final double NEAR_ZERO = 1.0E-9;
    private static final int BASE_PASSES = 2;

    public DamageOutcome scale(DamageEventModel event, double finalDamage, DamageMultiplier multiplier) {
        double target = Math.max(0.0, finalDamage * multiplier.value());
        if (scaledByComponents(event, multiplier) && close(event.finalDamage(), target)) {
            return DamageOutcome.SCALED;
        }
        for (int pass = 0; pass < BASE_PASSES; pass++) {
            double current = event.finalDamage();
            if (close(current, target)) {
                return DamageOutcome.SCALED;
            }
            double base = current > NEAR_ZERO ? event.baseDamage() * target / current : event.baseDamage() + target;
            event.baseDamage(Math.max(0.0, base));
        }
        return close(event.finalDamage(), target) ? DamageOutcome.SCALED : DamageOutcome.LEFT_AS_IS;
    }

    private static boolean scaledByComponents(DamageEventModel event, DamageMultiplier multiplier) {
        try {
            return event.scaleComponents(multiplier.value());
        } catch (RuntimeException unsupported) {
            return false;
        }
    }

    private static boolean close(double actual, double target) {
        return Math.abs(actual - target) <= TOLERANCE;
    }
}
