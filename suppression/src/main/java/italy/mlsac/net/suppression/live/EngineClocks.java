// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.time.MonotonicClock;
import italy.mlsac.net.suppression.time.WallClock;

public final class EngineClocks implements MonotonicClock, WallClock, EngineRandom {

    private static final String ENGINE_TIME = "suppression.clock";
    private static final String COOLDOWN_TIME = "suppression.cooldown";
    private static final String ENGINE_CHANCE = "suppression.random";

    private final Clock clock;
    private final RandomSource random;

    public EngineClocks(Clock clock, RandomSource random) {
        this.clock = clock;
        this.random = random;
    }

    @Override
    public long clockMillis() {
        return clock.monotonicMillis(ENGINE_TIME);
    }

    @Override
    public long epochMillis() {
        return clock.wallMillis(COOLDOWN_TIME);
    }

    @Override
    public double nextUnit() {
        return random.nextDouble(ENGINE_CHANCE);
    }
}
