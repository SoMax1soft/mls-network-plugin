// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import italy.mlsac.net.clock.RandomSource;

public final class ShuffleOrder {

    static final String PURPOSE = "troll.shuffle";

    private final RandomSource random;

    public ShuffleOrder(RandomSource random) {
        this.random = random;
    }

    public <T> void shuffle(T[] slots) {
        for (int last = slots.length - 1; last > 0; last--) {
            int pick = (int) random.nextLong(PURPOSE, last + 1L);
            T kept = slots[last];
            slots[last] = slots[pick];
            slots[pick] = kept;
        }
    }
}
