// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public record Capacity(double value) {

    private static final double MINIMUM = 30.0;

    public static Capacity ofThreshold(double violationThreshold) {
        return new Capacity(Math.max(MINIMUM, violationThreshold));
    }

    public double thresholdFor(int percent) {
        return percent * value / 100.0;
    }
}
