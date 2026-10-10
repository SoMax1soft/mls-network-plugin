// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.IntervalRange;

public final class IntervalSchedule {

    private boolean armed;
    private long nextAt;
    private IntervalRange range;

    public void keepFor(long now, IntervalRange current, EngineRandom random) {
        if (!armed) {
            armed = true;
            range = current;
            nextAt = now + current.draw(random);
            return;
        }
        if (!current.equals(range)) {
            range = current;
            if (nextAt > now + current.upperMillis()) {
                nextAt = now + current.draw(random);
            }
        }
    }

    public boolean due(long now) {
        return armed && now >= nextAt;
    }

    public void reschedule(long now, EngineRandom random) {
        nextAt = now + range.draw(random);
    }

    public void disarm() {
        armed = false;
    }

    public boolean armed() {
        return armed;
    }

    public long nextAt() {
        return nextAt;
    }
}
