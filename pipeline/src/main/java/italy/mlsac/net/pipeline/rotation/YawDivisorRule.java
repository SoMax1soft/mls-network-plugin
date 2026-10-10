// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

final class YawDivisorRule implements DivisorRule {
    private static final double PRECISION_SHARE = 0.0625;

    @Override
    public boolean accepts(double divisor, float precision) {
        return precision < divisor * PRECISION_SHARE;
    }

    @Override
    public double bucketWidth(float precision) {
        return Math.max(0.001, 2 * precision);
    }

    @Override
    public boolean refines(double coarse, float precision) {
        return precision < coarse * PRECISION_SHARE;
    }
}
