// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public record ScoringRules(double alertThreshold, double multiplier, double decayAmount, double decayThreshold,
                           double flagThreshold, double resetValue, double stepPercent) {

    public static final double DEFAULT_STEP_PERCENT = 0.33;

    public static ScoringRules pluginDefaults() {
        return new ScoringRules(0.80, 45.0, 1.5, 0.50, 40.0, 20.0, DEFAULT_STEP_PERCENT);
    }

    public double resetBuffer() {
        return Math.max(0.0, resetValue);
    }

    public boolean flags(double buffer) {
        return buffer >= flagThreshold;
    }
}
