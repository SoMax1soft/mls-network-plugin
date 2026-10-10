// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public final class BufferFormula {

    private BufferFormula() {
    }

    public static double next(double buffer, double probability, ScoringRules rules) {
        double growthThreshold = rules.alertThreshold();
        if (probability > growthThreshold) {
            return buffer + (probability - growthThreshold) * rules.multiplier();
        }
        double decayThreshold = rules.decayThreshold();
        if (probability < decayThreshold) {
            return Math.max(0.0, buffer - decrease(probability, decayThreshold, rules.decayAmount()));
        }
        return buffer;
    }

    private static double decrease(double probability, double decayThreshold, double amount) {
        if (decayThreshold <= 0.0) {
            return amount;
        }
        return amount * (decayThreshold - Math.min(decayThreshold, Math.max(0.0, probability))) / decayThreshold;
    }
}
