// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

record TickEndAllowance(double perSecond, double burst) {

    static TickEndAllowance scaled(int perSecond, int burst, double clientTicksPerSecond) {
        double scale = Math.max(1.0, clientTicksPerSecond / ClientTickPace.VANILLA_TICKS_PER_SECOND);
        return new TickEndAllowance(perSecond * scale, burst * scale);
    }
}
