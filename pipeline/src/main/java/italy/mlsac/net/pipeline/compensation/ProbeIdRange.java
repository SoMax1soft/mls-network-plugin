// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import java.util.HashSet;
import java.util.Set;

final class ProbeIdRange {
    static final int LOWEST = 0x4D4C0000;
    static final int HIGHEST = 0x4D4CFFFF;
    private static final int SPAN_MASK = HIGHEST - LOWEST;

    private final Set<Integer> handedOut = new HashSet<>();
    private int issued;

    static boolean owns(int id) {
        return id >= LOWEST && id <= HIGHEST;
    }

    int issue() {
        int id = LOWEST + (issued++ & SPAN_MASK);
        handedOut.add(id);
        return id;
    }

    boolean redeem(int id) {
        return handedOut.remove(id);
    }

    int notYetFlushed() {
        return handedOut.size();
    }
}
