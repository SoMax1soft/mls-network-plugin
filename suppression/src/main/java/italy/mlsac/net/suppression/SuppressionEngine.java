// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import italy.mlsac.net.suppression.chain.ChainBoard;
import italy.mlsac.net.suppression.chain.LadderChain;
import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.execution.BufferTriggerRunner;
import italy.mlsac.net.suppression.execution.CurrentPlan;
import italy.mlsac.net.suppression.execution.StageRunner;
import italy.mlsac.net.suppression.plan.BufferRulePlan;
import italy.mlsac.net.suppression.plan.LadderStep;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.suspect.BufferReadout;
import italy.mlsac.net.suppression.suspect.SuspectRegistry;
import italy.mlsac.net.suppression.time.MonotonicClock;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.BufferTrigger;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SuppressionEngine {

    private final CurrentPlan plan;
    private final SuspectRegistry registry;
    private final ChainBoard chains;
    private final StageRunner stages;
    private final BufferTriggerRunner bufferTriggers;
    private final SuspectExits exits;
    private final EffectWatcher watcher;
    private final SuppressionNotices notices;
    private final MonotonicClock clock;
    private final SweepCadence sweep;

    public SuppressionEngine(EngineParts parts) {
        this.plan = parts.plan();
        this.registry = parts.registry();
        this.chains = parts.chains();
        this.stages = parts.stages();
        this.bufferTriggers = parts.bufferTriggers();
        this.exits = parts.exits();
        this.watcher = parts.watcher();
        this.notices = parts.notices();
        this.clock = parts.clock();
        this.sweep = new SweepCadence(clock.clockMillis());
    }

    public SuppressionPlan plan() {
        return plan.get();
    }

    public void onFlag(FlagEvent flag) {
        if (flag.origin() == SuspectOrigin.BENCH) {
            return;
        }
        Optional<LadderStep> step = plan.get().ladder().stepFor(flag.context().vl());
        if (step.isEmpty()) {
            return;
        }
        long now = clock.clockMillis();
        LadderChain chain = LadderChain.start(flag.context(), step.get(), now);
        chains.start(chain, now).forEach(stages::run);
    }

    public Optional<BufferTrigger> onAnswer(BufferAnswer answer) {
        if (!answer.evaluatesRules()) {
            return Optional.empty();
        }
        SuppressionPlan current = plan.get();
        UUID id = answer.context().suspectId();
        List<BufferRulePlan> fired = registry.suspectStateOf(id, current)
                .evaluate(answer.context().buffer(), clock.clockMillis(), current);
        if (fired.isEmpty()) {
            return Optional.empty();
        }
        BufferTrigger trigger = new BufferTrigger(answer.context(), fired);
        bufferTriggers.run(trigger);
        return Optional.of(trigger);
    }

    public void onBufferLowered(UUID suspectId, double buffer) {
        registry.existing(suspectId).ifPresent(state -> state.rearmBelow(buffer));
    }

    public void tick() {
        long now = clock.clockMillis();
        chains.due(now).forEach(stages::run);
        if (sweep.due(now)) {
            registry.sweep(now, watcher::effectsChanged);
            exits.sweepIdle();
        }
    }

    public void quitSeen(UUID suspectId) {
        EffectSnapshot snapshot = registry.leave(suspectId, clock.clockMillis(), plan.get());
        watcher.effectsChanged(suspectId, snapshot);
        exits.quit(suspectId);
    }

    public void onJoin(UUID suspectId) {
        watcher.effectsChanged(suspectId, registry.rejoin(suspectId, clock.clockMillis()));
    }

    public void applyPlan(SuppressionPlan next, BufferReadout buffers) {
        plan.replace(next);
        registry.rebuildLatches(next, buffers);
    }

    public List<Integer> firedRulePercents(UUID suspectId) {
        return registry.existing(suspectId).map(state -> state.firedPercents()).orElse(List.of());
    }

    public int pendingCommands() {
        return chains.pendingCommands();
    }

    public void release() {
        int pending = chains.pendingCommands();
        if (pending > 0) {
            notices.warn("[Suppression] Dropped " + pending + " pending suppression command(s) at shutdown");
        }
        chains.clear();
        registry.clear();
        exits.freeEverySuspect();
    }
}
