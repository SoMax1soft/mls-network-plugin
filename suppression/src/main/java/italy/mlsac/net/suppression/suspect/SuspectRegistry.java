// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.suspect;

import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.trigger.RuleLatches;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class SuspectRegistry {

    private final Map<UUID, SuspectState> states = new ConcurrentHashMap<>();
    private final RetentionPolicy retention;

    public SuspectRegistry(RetentionPolicy retention) {
        this.retention = retention;
    }

    public SuspectState suspectStateOf(UUID suspectId, SuppressionPlan plan) {
        return states.computeIfAbsent(suspectId, id -> new SuspectState(RuleLatches.armed(plan)));
    }

    public Optional<SuspectState> existing(UUID suspectId) {
        return Optional.ofNullable(states.get(suspectId));
    }

    public EffectSnapshot snapshotOf(UUID suspectId) {
        SuspectState state = states.get(suspectId);
        return state == null ? EffectSnapshot.empty() : state.snapshot();
    }

    public EffectSnapshot leave(UUID suspectId, long now, SuppressionPlan plan) {
        return suspectStateOf(suspectId, plan).leave(now);
    }

    public EffectSnapshot rejoin(UUID suspectId, long now) {
        SuspectState state = states.get(suspectId);
        if (state == null) {
            return EffectSnapshot.empty();
        }
        if (retention.expired(state.offlineFor(now))) {
            states.remove(suspectId, state);
            return EffectSnapshot.empty();
        }
        return state.rejoin(now);
    }

    public Optional<SuspectState> retainedOffline(UUID suspectId, long now) {
        SuspectState state = states.get(suspectId);
        if (state == null || state.online() || retention.expired(state.offlineFor(now))) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    public void sweep(long now, BiConsumer<UUID, EffectSnapshot> changed) {
        states.forEach((id, state) -> {
            if (retention.expired(state.offlineFor(now))) {
                states.remove(id, state);
                changed.accept(id, EffectSnapshot.empty());
            } else {
                changed.accept(id, state.sweep(now));
            }
        });
    }

    public void rebuildLatches(SuppressionPlan plan, BufferReadout buffers) {
        states.forEach((id, state) -> state.replaceLatches(
                RuleLatches.forCurrentBuffer(plan, buffers.currentBuffer(id).orElse(0.0))));
    }

    public int size() {
        return states.size();
    }

    public void clear() {
        states.clear();
    }
}
