// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

import italy.mlsac.net.suppression.chance.EngineRandom;

public record IntervalRange(long lowerMillis, long upperMillis) {

    public long draw(EngineRandom random) {
        return Math.round(random.uniform(lowerMillis, upperMillis));
    }
}
