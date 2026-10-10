// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.time.MonotonicClock;
import italy.mlsac.net.suppression.time.WallClock;

public final class ManualClock implements MonotonicClock, WallClock {

    private long now;

    public void set(long millis) {
        now = millis;
    }

    public void advance(long millis) {
        now += millis;
    }

    @Override
    public long clockMillis() {
        return now;
    }

    @Override
    public long epochMillis() {
        return 1_700_000_000_000L + now;
    }
}
