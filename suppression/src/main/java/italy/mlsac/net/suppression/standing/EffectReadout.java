// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.standing;

import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.lag.LagLevels;
import italy.mlsac.net.suppression.suspect.SuspectRegistry;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.UUID;

public final class EffectReadout {

    private final SuspectRegistry registry;
    private final SuspectStanding standing;
    private final MonotonicClock clock;

    public EffectReadout(SuspectRegistry registry, SuspectStanding standing, MonotonicClock clock) {
        this.registry = registry;
        this.standing = standing;
        this.clock = clock;
    }

    public boolean touches(UUID attackerId, UUID victimId) {
        return carries(attackerId) || carries(victimId);
    }

    public int dealtPercent(UUID suspectId) {
        if (!carries(suspectId)) {
            return 0;
        }
        int percent = registry.snapshotOf(suspectId).dealtPercent(clock.clockMillis());
        return percent == 0 || standing.shielded(suspectId) ? 0 : percent;
    }

    public int takenPercent(UUID suspectId) {
        if (!carries(suspectId)) {
            return 0;
        }
        int percent = registry.snapshotOf(suspectId).takenPercent(clock.clockMillis());
        return percent == 0 || standing.shielded(suspectId) ? 0 : percent;
    }

    public LagLevels lagLevels(UUID suspectId) {
        if (!carries(suspectId)) {
            return LagLevels.none();
        }
        LagLevels levels = registry.snapshotOf(suspectId).lagLevels(clock.clockMillis());
        return levels.isEmpty() || standing.shielded(suspectId) || standing.lagExempt(suspectId) ? LagLevels.none()
                : levels;
    }

    private boolean carries(UUID suspectId) {
        if (suspectId == null) {
            return false;
        }
        EffectSnapshot snapshot = registry.snapshotOf(suspectId);
        return !snapshot.isEmpty();
    }
}
