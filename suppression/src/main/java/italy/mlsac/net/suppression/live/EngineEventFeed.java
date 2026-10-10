// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.config.ConfigApplied;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.detection.BufferChangeCause;
import italy.mlsac.net.detection.BufferChanged;
import italy.mlsac.net.detection.PlayerFlagged;
import italy.mlsac.net.player.PlayerJoined;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.suppression.SuppressionEngine;
import italy.mlsac.net.suppression.route.ResponseSwitch;
import italy.mlsac.net.suppression.suspect.BufferReadout;
import italy.mlsac.net.suppression.trigger.AnswerKind;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import italy.mlsac.net.suppression.trigger.SuspectOrigin;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class EngineEventFeed {

    private final EventBus bus;
    private final TaskScheduler scheduler;
    private final Supplier<EffectiveConfig> config;
    private final SuppressionEngine engine;
    private final ResponseSwitch responses;
    private final PlanTranslation plans;
    private final BufferReadout buffers;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private Optional<TaskTicket> ticker = Optional.empty();

    public EngineEventFeed(EventBus bus, TaskScheduler scheduler, Supplier<EffectiveConfig> config,
                           SuppressionEngine engine, ResponseSwitch responses, PlanTranslation plans,
                           BufferReadout buffers) {
        this.bus = bus;
        this.scheduler = scheduler;
        this.config = config;
        this.engine = engine;
        this.responses = responses;
        this.plans = plans;
        this.buffers = buffers;
    }

    public void start() {
        applyPlan(config.get());
        subscriptions.add(bus.subscribe(ConfigApplied.class, applied -> applyPlan(applied.current())));
        subscriptions.add(bus.subscribe(BufferChanged.class, this::bufferChanged));
        subscriptions.add(bus.subscribe(PlayerFlagged.class, this::flagged));
        subscriptions.add(bus.subscribe(PlayerJoined.class, joined -> engine.onJoin(joined.player())));
        subscriptions.add(bus.subscribe(PlayerLeft.class, left -> scheduler.submit(TaskLane.MAIN,
                TaskTiming.immediately(), () -> engine.quitSeen(left.player()))));
        ticker = Optional.of(scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(1L, 1L), engine::tick));
    }

    public void stop() {
        ticker.ifPresent(TaskTicket::cancel);
        ticker = Optional.empty();
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
        engine.release();
    }

    private void applyPlan(EffectiveConfig current) {
        responses.apply(plans.translate(current), buffers);
    }

    private void bufferChanged(BufferChanged change) {
        if (change.cause() == BufferChangeCause.VERDICT) {
            responses.onAnswer(new BufferAnswer(context(change), AnswerKind.SCORING, SuspectOrigin.PLAYER));
            return;
        }
        responses.onBufferLowered(change.player(), change.buffer());
    }

    private void flagged(PlayerFlagged flag) {
        TriggerContext context = new TriggerContext(flag.player(), flag.playerNick(), flag.offenceLevel(),
                flag.probability(), flag.buffer());
        responses.onFlag(new FlagEvent(context, flag.virtualPlayer() ? SuspectOrigin.BENCH : SuspectOrigin.PLAYER));
    }

    private static TriggerContext context(BufferChanged change) {
        return new TriggerContext(change.player(), change.playerNick(), change.offenceLevel(), change.probability(),
                change.buffer());
    }
}
