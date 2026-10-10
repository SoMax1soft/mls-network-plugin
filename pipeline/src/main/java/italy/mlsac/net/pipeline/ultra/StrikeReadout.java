// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

public record StrikeReadout(double kickAtAttacks, double attackErrorRatio, double onTargetShort,
        double onTargetLong) {

    static final int SIGNALS = 4;

    static StrikeReadout of(double kickAtAttacks, double attackErrorRatio, double onTargetShort,
            double onTargetLong) {
        return new StrikeReadout(finiteOrNaN(kickAtAttacks), finiteOrNaN(attackErrorRatio),
                finiteOrNaN(onTargetShort), finiteOrNaN(onTargetLong));
    }

    void copyInto(double[] target, int offset) {
        target[offset] = kickAtAttacks;
        target[offset + 1] = attackErrorRatio;
        target[offset + 2] = onTargetShort;
        target[offset + 3] = onTargetLong;
    }

    private static double finiteOrNaN(double signal) {
        return Double.isInfinite(signal) ? Double.NaN : signal;
    }
}
