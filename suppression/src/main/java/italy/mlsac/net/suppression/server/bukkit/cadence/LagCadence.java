// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.suppression.lag.runtime.LagTicker;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.Optional;
import java.util.UUID;

public final class LagCadence {

    private final TickRoster roster;
    private final LagTicker ticker;
    private final LagSubjects subjects;
    private final MonotonicClock clock;

    public LagCadence(TickRoster roster, LagTicker ticker, LagSubjects subjects, MonotonicClock clock) {
        this.roster = roster;
        this.ticker = ticker;
        this.subjects = subjects;
        this.clock = clock;
    }

    public void serverTicked(ServerTick tick) {
        for (UUID suspectId : roster.members()) {
            advance(suspectId, tick.tick());
        }
    }

    public void playerTicked(PlayerTick tick) {
        if (roster.watches(tick.player())) {
            advance(tick.player(), tick.tick());
        }
    }

    private void advance(UUID suspectId, long tick) {
        Optional<LagSubject> subject = subjects.subjectOf(suspectId);
        if (subject.isEmpty()) {
            roster.drop(suspectId);
            return;
        }
        long now = clock.clockMillis();
        ticker.tick(suspectId, subject.get().frame(now, tick), subject.get().actuator());
        roster.settle(suspectId, now);
    }
}
