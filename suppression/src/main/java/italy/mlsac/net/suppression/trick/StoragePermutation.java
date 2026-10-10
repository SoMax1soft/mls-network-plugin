// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import italy.mlsac.net.suppression.chance.EngineRandom;

public final class StoragePermutation {

    private final EngineRandom random;

    public StoragePermutation(EngineRandom random) {
        this.random = random;
    }

    public int[] draw(int size) {
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        for (int i = size - 1; i > 0; i--) {
            int j = random.belowBound(i + 1);
            int held = order[i];
            order[i] = order[j];
            order[j] = held;
        }
        return order;
    }
}
