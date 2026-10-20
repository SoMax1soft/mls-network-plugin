// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

public final class SplitMix64 {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long FIRST_MIX = 0xBF58476D1CE4E5B9L;
    private static final long SECOND_MIX = 0x94D049BB133111EBL;
    private static final double UNIT_STEP = 0x1.0p-53;

    private long state;

    public SplitMix64(long seed) {
        this.state = seed;
    }

    public long next() {
        state += GOLDEN_GAMMA;
        long z = state;
        z = (z ^ (z >>> 30)) * FIRST_MIX;
        z = (z ^ (z >>> 27)) * SECOND_MIX;
        return z ^ (z >>> 31);
    }

    public double nextUnit() {
        return (next() >>> 11) * UNIT_STEP;
    }
}
