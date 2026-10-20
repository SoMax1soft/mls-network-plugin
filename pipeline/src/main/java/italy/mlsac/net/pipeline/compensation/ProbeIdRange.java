// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import java.util.BitSet;

final class ProbeIdRange {
    static final int LOWEST = 0x4D4C0000;
    static final int HIGHEST = 0x4D4CFFFF;
    private static final int SPAN_MASK = HIGHEST - LOWEST;

    private final BitSet handedOut = new BitSet(SPAN_MASK + 1);
    private int outstanding;
    private int issued;

    static boolean owns(int id) {
        return id >= LOWEST && id <= HIGHEST;
    }

    int issue() {
        int offset = issued++ & SPAN_MASK;
        if (!handedOut.get(offset)) {
            handedOut.set(offset);
            outstanding++;
        }
        return LOWEST + offset;
    }

    boolean redeem(int id) {
        if (!owns(id)) {
            return false;
        }
        int offset = id - LOWEST;
        if (!handedOut.get(offset)) {
            return false;
        }
        handedOut.clear(offset);
        outstanding--;
        return true;
    }

    int notYetFlushed() {
        return outstanding;
    }
}
