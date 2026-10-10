// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

record LastAppended(double turn, double turnBefore, double lead, double viewYaw, double viewPitch,
        double relativeMotion, boolean targetValid, boolean bothValid, boolean onBody, int steadyBits) {

    static final LastAppended BLANK = new LastAppended(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, false, 0);

    private static final int STEADY_MASK = 0b111;

    boolean steadyThreeInARow() {
        return steadyBits == STEADY_MASK;
    }

    int steadyBitsWith(boolean steady) {
        return ((steadyBits << 1) | (steady ? 1 : 0)) & STEADY_MASK;
    }
}
