// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public record AlertStep(double fraction, long key) {

    public static final String LATCH_PREFIX = "alert-step-";

    public static AlertStep of(double fraction) {
        return new AlertStep(fraction, Math.round(fraction * 1000.0));
    }

    public String latchName() {
        return LATCH_PREFIX + key;
    }

    public double threshold(double flagThreshold) {
        return flagThreshold * fraction;
    }

    public long percent() {
        return Math.round(fraction * 100.0);
    }
}
