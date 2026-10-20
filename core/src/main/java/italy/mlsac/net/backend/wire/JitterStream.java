// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

public final class JitterStream {

    private final SplitMix64 generator;

    public JitterStream(WireSeed seed) {
        this.generator = new SplitMix64(seed.jitterBits());
    }

    public double draw() {
        return generator.nextUnit() * 2.0 - 1.0;
    }
}
