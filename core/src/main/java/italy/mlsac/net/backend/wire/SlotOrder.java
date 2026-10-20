// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

public final class SlotOrder {

    private final int[] sources;

    private SlotOrder(int[] sources) {
        this.sources = sources;
    }

    public static SlotOrder shuffled(WireSeed seed, int width) {
        SplitMix64 generator = new SplitMix64(seed.bits());
        int[] order = new int[width];
        for (int slot = 0; slot < width; slot++) {
            order[slot] = slot;
        }
        for (int i = width - 1; i >= 1; i--) {
            int k = (int) Long.remainderUnsigned(generator.next() >>> 1, i + 1);
            int held = order[i];
            order[i] = order[k];
            order[k] = held;
        }
        return new SlotOrder(order);
    }

    public int width() {
        return sources.length;
    }

    public int sourceOf(int slot) {
        return sources[slot];
    }

    public double[] arrange(double[] extended) {
        if (extended.length != sources.length) {
            throw new IllegalArgumentException("row holds " + extended.length + " values, " + sources.length
                    + " expected");
        }
        double[] placed = new double[sources.length];
        for (int slot = 0; slot < sources.length; slot++) {
            placed[slot] = extended[sources[slot]];
        }
        return placed;
    }
}
