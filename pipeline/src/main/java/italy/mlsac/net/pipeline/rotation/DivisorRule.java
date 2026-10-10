// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rotation;

interface DivisorRule {
    boolean accepts(double divisor, float precision);

    double bucketWidth(float precision);

    boolean refines(double coarse, float precision);
}
