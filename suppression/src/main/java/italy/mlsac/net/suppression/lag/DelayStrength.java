// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import italy.mlsac.net.suppression.chance.EngineRandom;

public record DelayStrength(long baseMillis) {

    private static final double LOWEST_FACTOR = 0.8;
    private static final double HIGHEST_FACTOR = 1.2;

    public long drawDelayMillis(EngineRandom random) {
        return Math.round(baseMillis * random.uniform(LOWEST_FACTOR, HIGHEST_FACTOR));
    }
}
