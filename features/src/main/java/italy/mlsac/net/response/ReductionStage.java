// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

public record ReductionStage(double buffer, double cutPercent, int lastsSeconds, boolean enabled) {

    public String latchName() {
        return "dr-" + buffer;
    }

    public double clampedPercent() {
        return Math.max(0.0, Math.min(100.0, cutPercent));
    }

    public boolean usable() {
        return enabled && buffer > 0 && lastsSeconds > 0 && cutPercent > 0;
    }
}
