// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.chance;

@FunctionalInterface
public interface EngineRandom {

    double nextUnit();

    default double uniform(double min, double max) {
        return min + nextUnit() * (max - min);
    }

    default int belowBound(int bound) {
        int value = (int) Math.floor(nextUnit() * bound);
        return Math.max(0, Math.min(bound - 1, value));
    }
}
