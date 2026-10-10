// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

final class PitchDivisorRule implements DivisorRule {
    @Override
    public boolean accepts(double divisor, float precision) {
        return true;
    }

    @Override
    public double bucketWidth(float precision) {
        return 0.001;
    }

    @Override
    public boolean refines(double coarse, float precision) {
        return true;
    }
}
