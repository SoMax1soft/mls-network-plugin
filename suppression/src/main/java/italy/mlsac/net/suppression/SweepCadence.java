// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

final class SweepCadence {

    static final long INTERVAL_MILLIS = 60_000L;

    private long lastSweepAt;

    SweepCadence(long beganAt) {
        this.lastSweepAt = beganAt;
    }

    boolean due(long now) {
        if (now - lastSweepAt < INTERVAL_MILLIS) {
            return false;
        }
        lastSweepAt = now;
        return true;
    }
}
