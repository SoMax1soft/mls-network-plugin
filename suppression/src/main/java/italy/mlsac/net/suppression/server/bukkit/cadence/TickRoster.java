// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TickRoster implements EffectWatcher {

    private final MonotonicClock clock;
    private final Map<UUID, EffectSnapshot> watched = new ConcurrentHashMap<>();

    public TickRoster(MonotonicClock clock) {
        this.clock = clock;
    }

    @Override
    public void effectsChanged(UUID suspectId, EffectSnapshot snapshot) {
        if (snapshot.lagLevels(clock.clockMillis()).needsTickWork()) {
            watched.put(suspectId, snapshot);
        } else {
            watched.computeIfPresent(suspectId, (id, previous) -> snapshot);
        }
    }

    public boolean watches(UUID suspectId) {
        return watched.containsKey(suspectId);
    }

    public List<UUID> members() {
        return List.copyOf(watched.keySet());
    }

    public void settle(UUID suspectId, long now) {
        EffectSnapshot snapshot = watched.get(suspectId);
        if (snapshot != null && !snapshot.lagLevels(now).needsTickWork()) {
            watched.remove(suspectId, snapshot);
        }
    }

    public void drop(UUID suspectId) {
        watched.remove(suspectId);
    }

    public void clear() {
        watched.clear();
    }
}
