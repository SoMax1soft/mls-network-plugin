// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.lag.LagLevelSource;
import italy.mlsac.net.suppression.lag.packet.HeldDisposal;
import italy.mlsac.net.suppression.trick.LaunchObserver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LagRuntimes<P> implements LaunchObserver {

    private final Map<UUID, LagRuntime<P>> runtimes = new ConcurrentHashMap<>();
    private final HeldDisposal<P> disposal;

    public LagRuntimes() {
        this(HeldDisposal.forgetting());
    }

    public LagRuntimes(HeldDisposal<P> disposal) {
        this.disposal = disposal;
    }

    public LagRuntime<P> runtimeOf(UUID suspectId) {
        return runtimes.computeIfAbsent(suspectId, id -> new LagRuntime<>());
    }

    public Optional<LagRuntime<P>> existing(UUID suspectId) {
        return Optional.ofNullable(runtimes.get(suspectId));
    }

    public int quit(UUID suspectId) {
        LagRuntime<P> runtime = runtimes.remove(suspectId);
        return runtime == null ? 0 : dispose(runtime);
    }

    public int died(UUID suspectId) {
        return existing(suspectId).map(runtime -> {
            runtime.pauses().died();
            runtime.forgetMovement();
            return dispose(runtime);
        }).orElse(0);
    }

    public int respawned(UUID suspectId, long tick) {
        return existing(suspectId).map(runtime -> {
            runtime.pauses().respawned(tick);
            runtime.forgetMovement();
            return dispose(runtime);
        }).orElse(0);
    }

    public int worldSwitched(UUID suspectId, long tick) {
        return existing(suspectId).map(runtime -> {
            runtime.pauses().moved(tick);
            runtime.forgetMovement();
            return dispose(runtime);
        }).orElse(0);
    }

    public void teleported(UUID suspectId, long tick) {
        existing(suspectId).ifPresent(runtime -> runtime.pauses().teleported(tick));
    }

    public void teleportedByOthers(UUID suspectId, long tick) {
        existing(suspectId).ifPresent(runtime -> {
            runtime.pauses().moved(tick);
            runtime.forgetMovement();
        });
    }

    public void knockedBack(UUID suspectId, long tick) {
        existing(suspectId).ifPresent(runtime -> runtime.pauses().knockedBack(tick));
    }

    @Override
    public void launched(UUID suspectId) {
        existing(suspectId).ifPresent(runtime -> runtime.pauses().launched());
    }

    public void retireIdle(LagLevelSource levels) {
        runtimes.entrySet().removeIf(entry -> levels.lagLevels(entry.getKey()).isEmpty()
                && !entry.getValue().heldPackets().hasQueued()
                && !entry.getValue().pauses().dead());
    }

    public int discardAll() {
        int dropped = 0;
        for (LagRuntime<P> runtime : runtimes.values()) {
            dropped += dispose(runtime);
        }
        runtimes.clear();
        return dropped;
    }

    private int dispose(LagRuntime<P> runtime) {
        List<P> dropped = runtime.heldPackets().discard();
        dropped.forEach(disposal::dispose);
        return dropped.size();
    }

    public int size() {
        return runtimes.size();
    }
}
