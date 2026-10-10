// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

public final class SetbackState {

    private final PositionHistory history = new PositionHistory();
    private final IntervalSchedule schedule = new IntervalSchedule();

    public PositionHistory history() {
        return history;
    }

    public IntervalSchedule schedule() {
        return schedule;
    }

    public void reset() {
        history.clear();
        schedule.disarm();
    }
}
