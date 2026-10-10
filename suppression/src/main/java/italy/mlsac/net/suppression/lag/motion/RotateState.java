// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

public final class RotateState {

    private final IntervalSchedule schedule = new IntervalSchedule();
    private int lastSyncId;

    public IntervalSchedule schedule() {
        return schedule;
    }

    public int nextSyncId() {
        lastSyncId = lastSyncId == Integer.MIN_VALUE ? -1 : lastSyncId - 1;
        return lastSyncId;
    }

    public void reset() {
        schedule.disarm();
    }
}
