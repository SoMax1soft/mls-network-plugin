// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features;

import italy.mlsac.net.alerting.ProbTracking;
import italy.mlsac.net.backend.notice.InterServerAlert;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.config.ConfigApplied;
import italy.mlsac.net.config.ConfigChangeCause;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.detection.ModelVerdict;
import italy.mlsac.net.detection.WindowSubmitted;
import italy.mlsac.net.features.live.NametagStage;
import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.menu.MenuDesk;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.player.PlayerJoined;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class FeatureRhythm {

    private static final long STAFF_JOIN_DELAY_TICKS = 20L;
    private static final long ROSTER_PURGE_TICKS = 1200L;

    private final EventBus bus;
    private final TaskScheduler scheduler;
    private final PluginLog log;
    private final FeatureWiring wiring;
    private final MenuDesk menus;
    private final NametagStage nametags;
    private final ProbTracking tracking;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<TaskTicket> timers = new ArrayList<>();
    private Optional<TaskTicket> decayTimer = Optional.empty();

    FeatureRhythm(CoreServices core, FeatureWiring wiring, MenuDesk menus, NametagStage nametags,
                  ProbTracking tracking) {
        this.bus = core.bus();
        this.scheduler = core.scheduler();
        this.log = core.log();
        this.wiring = wiring;
        this.menus = menus;
        this.nametags = nametags;
        this.tracking = tracking;
    }

    void start() {
        loadAnimations();
        subscriptions.add(bus.subscribe(ConfigApplied.class, this::configApplied));
        subscriptions.add(bus.subscribe(WindowSubmitted.class, wiring.verdicts::submitted));
        subscriptions.add(bus.subscribe(ModelVerdict.class, wiring.verdicts::answered));
        subscriptions.add(bus.subscribe(BadPacketObserved.class, wiring.badPackets::observed));
        subscriptions.add(bus.subscribe(InterServerAlert.class, wiring.crossServer::received));
        subscriptions.add(bus.subscribe(ServerTick.class, tick -> wiring.admission.everyOnline()));
        subscriptions.add(bus.subscribe(PlayerTick.class, tick -> wiring.admission.one(tick.player())));
        subscriptions.add(bus.subscribe(PlayerJoined.class, joined -> joined(joined.player())));
        subscriptions.add(bus.subscribe(PlayerLeft.class, left -> left(left.player())));
        timers.add(scheduler.submit(TaskLane.BACKGROUND, TaskTiming.repeating(ROSTER_PURGE_TICKS, ROSTER_PURGE_TICKS),
                wiring.admission::purge));
        restartDecay();
    }

    void stop() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
        timers.forEach(TaskTicket::cancel);
        timers.clear();
        decayTimer.ifPresent(TaskTicket::cancel);
        decayTimer = Optional.empty();
        tracking.haltEvery();
        wiring.director.abandonAll();
        wiring.animationTicker.haltEvery();
        wiring.removalLog.clear();
        wiring.verdicts.clear();
        wiring.badPackets.clear();
        wiring.roster.clear();
    }

    private void configApplied(ConfigApplied applied) {
        wiring.settings.refresh(applied.current());
        wiring.legacyResponses.configurationChanged();
        restartDecay();
        if (applied.cause() == ConfigChangeCause.RELOAD) {
            wiring.director.abandonAll();
            tracking.haltEvery();
            loadAnimations();
            nametags.rebuild();
        }
    }

    private void restartDecay() {
        decayTimer.ifPresent(TaskTicket::cancel);
        long every = Math.max(1L, wiring.settings.get().vlDecay().everyTicks());
        decayTimer = Optional.of(scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(every, every), wiring.decay));
    }

    private void loadAnimations() {
        try {
            int loaded = wiring.library.load(wiring.animationFiles.get());
            log.debug(LogTopic.GENERAL, () -> "Animation library ready: " + loaded + " file(s)");
        } catch (RuntimeException failure) {
            log.error(LogTopic.GENERAL, "Ban animations could not be loaded: " + failure.getMessage(), failure);
        }
    }

    private void joined(UUID player) {
        scheduler.submit(TaskLane.following(player), TaskTiming.after(STAFF_JOIN_DELAY_TICKS),
                () -> wiring.enrollment.joinDelayElapsed(player));
    }

    private void left(UUID player) {
        wiring.enrollment.left(player);
        wiring.admission.departed(player);
        wiring.badPackets.left(player);
        wiring.punishCooldown.forget(player);
        wiring.reductions.forget(player);
        wiring.trickCooldowns.forget(player);
        wiring.director.quit(player);
        menus.forget(player);
    }
}
