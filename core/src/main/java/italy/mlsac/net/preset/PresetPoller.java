// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSink;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.core.PresetMode;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PresetPoller {

    public static final long FIRST_FETCH_TICKS = 40L;
    public static final long CADENCE_TICKS = 1200L;
    private static final long AUTH_WARNING_GAP_MILLIS = 30 * 60 * 1000L;

    private final TaskScheduler scheduler;
    private final PresetFetcher fetcher;
    private final PresetLayout layout;
    private final ConfigAssembler assembler;
    private final PresetSink sink;
    private final MissingPresetNotices notices;
    private final PluginLog log;
    private final Clock clock;
    private final AtomicBoolean unanswered = new AtomicBoolean();
    private volatile boolean stopped;
    private volatile String activeDigest = "";
    private volatile long lastAuthWarning = Long.MIN_VALUE / 2;
    private volatile CoreSettings core;
    private TaskTicket timer;

    public PresetPoller(TaskScheduler scheduler, PresetFetcher fetcher, PresetLayout layout, ConfigAssembler assembler,
                        PresetSink sink, MissingPresetNotices notices, PluginLog log, Clock clock) {
        this.scheduler = scheduler;
        this.fetcher = fetcher;
        this.layout = layout;
        this.assembler = assembler;
        this.sink = sink;
        this.notices = notices;
        this.log = log;
        this.clock = clock;
    }

    public synchronized void start(CoreSettings settings, boolean detectionEnabled) {
        this.core = settings;
        this.stopped = false;
        this.activeDigest = "";
        notices.forget();
        String name = settings.preset().configuredName();
        if (settings.preset().mode() == PresetMode.DISABLED) {
            log.info(LogTopic.PRESET, "Remote config disabled (remote-config.preset: " + name
                    + ") - using local configuration");
            if (detectionEnabled) {
                timer = scheduler.submit(TaskLane.MAIN, TaskTiming.after(FIRST_FETCH_TICKS), () -> notices.report(MissingReason.DISABLED, name));
            }
            return;
        }
        log.debug(LogTopic.PRESET, () -> "polling " + (name.isEmpty() ? "the only preset of the account" : "preset '" + name + "'")
                + " once a minute");
        timer = scheduler.submit(TaskLane.BACKGROUND, TaskTiming.repeating(FIRST_FETCH_TICKS, CADENCE_TICKS), this::fetchNow);
    }

    public synchronized void stop() {
        stopped = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void fetchNow() {
        CoreSettings settings = core;
        if (stopped || settings == null || !settings.accessKey().configured()
                || settings.preset().mode() == PresetMode.DISABLED || !unanswered.compareAndSet(false, true)) {
            return;
        }
        fetcher.fetch(settings.preset(), settings.accessKey().value()).whenComplete((answer, error) -> {
            try {
                if (!stopped && answer != null) {
                    handle(answer, settings);
                }
            } finally {
                unanswered.set(false);
            }
        });
    }

    private void handle(PresetAnswer answer, CoreSettings settings) {
        if (answer instanceof PresetAnswer.Delivered delivered) {
            delivered(delivered, settings);
        } else if (answer instanceof PresetAnswer.Unavailable unavailable) {
            unavailable(unavailable.reason(), settings);
        } else if (answer instanceof PresetAnswer.Failed failed) {
            failed(failed);
        }
    }

    private void delivered(PresetAnswer.Delivered delivered, CoreSettings settings) {
        ConfigTree effective = layout.toEffective(delivered.config());
        if (assembler.countUsableValues(effective) == 0) {
            unavailable(MissingReason.NOTHING_USABLE, settings);
            return;
        }
        notices.forget();
        if (delivered.hash().equals(activeDigest)) {
            return;
        }
        activeDigest = delivered.hash();
        Optional<String> name = delivered.name().filter(n -> !n.isEmpty())
                .or(() -> settings.preset().bodyValue());
        log.info(LogTopic.PRESET, "Applying preset '" + name.orElse("auto") + "' (" + delivered.hash() + ")");
        PresetSnapshot snapshot = new PresetSnapshot(name, delivered.hash(), effective);
        scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), () -> sink.apply(Optional.of(snapshot)));
    }

    private void unavailable(MissingReason reason, CoreSettings settings) {
        String name = settings.preset().configuredName();
        if (!activeDigest.isEmpty()) {
            activeDigest = "";
            if (settings.preset().mode() == PresetMode.NAMED) {
                log.info(LogTopic.PRESET, "Preset '" + name + "' is unavailable - falling back to local configuration");
            }
            scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), () -> sink.apply(Optional.empty()));
        }
        if (reason.notifies()) {
            notices.report(reason, name);
        } else {
            log.debug(LogTopic.PRESET, () -> "config endpoint not available on this backend");
        }
    }

    private void failed(PresetAnswer.Failed failed) {
        if (!failed.authRejected()) {
            log.debug(LogTopic.PRESET, () -> "preset fetch failed: " + failed.cause());
            return;
        }
        long now = clock.wallMillis("preset.auth_warning");
        if (now - lastAuthWarning >= AUTH_WARNING_GAP_MILLIS) {
            lastAuthWarning = now;
            log.warn(LogTopic.PRESET, "The site refused the preset download (" + failed.cause() + "); is detection.api-key right?");
        }
    }
}
