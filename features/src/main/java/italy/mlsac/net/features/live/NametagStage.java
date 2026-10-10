// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.nametag.GameRelease;
import italy.mlsac.net.nametag.NametagDirector;
import italy.mlsac.net.nametag.NametagIdentities;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.nametag.NametagSettings;
import italy.mlsac.net.nametag.NametagText;
import italy.mlsac.net.nametag.SuspectTags;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.workload.WorkloadCounters;
import java.util.Optional;
import java.util.function.Supplier;

public final class NametagStage implements Supplier<Optional<NametagDirector>> {

    private static final long REDRAW_PERIOD_TICKS = 10L;

    private final Supplier<NametagSettings> settings;
    private final GameRelease server;
    private final NametagPorts ports;
    private final TaskScheduler scheduler;
    private final PluginLog log;
    private final boolean folia;
    private final WorkloadCounters counters;
    private volatile Optional<NametagDirector> director = Optional.empty();
    private Optional<TaskTicket> refresh = Optional.empty();

    public NametagStage(Supplier<NametagSettings> settings, GameVersion serverVersion, NametagPorts ports,
                        TaskScheduler scheduler, PluginLog log, boolean folia, WorkloadCounters counters) {
        this.settings = settings;
        this.server = new GameRelease(serverVersion.major(), serverVersion.minor(), serverVersion.patch());
        this.ports = ports;
        this.scheduler = scheduler;
        this.log = log;
        this.folia = folia;
        this.counters = counters;
    }

    public synchronized void start() {
        if (folia) {
            log.warn(LogTopic.GENERAL, "Suspect nametags stay off: this server runs Folia, where they are "
                    + "not supported.");
            return;
        }
        rebuild();
        refresh = Optional.of(scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(REDRAW_PERIOD_TICKS, REDRAW_PERIOD_TICKS),
                this::redraw));
    }

    public synchronized void rebuild() {
        if (folia) {
            return;
        }
        director.ifPresent(NametagDirector::stop);
        director = Optional.of(new NametagDirector(settings, server, new NametagText(new HexColorNormalizer()),
                new SuspectTags(new NametagIdentities()), ports, counters));
    }

    public synchronized void stop() {
        refresh.ifPresent(TaskTicket::cancel);
        refresh = Optional.empty();
        director.ifPresent(NametagDirector::stop);
        director = Optional.empty();
    }

    private void redraw() {
        Optional<NametagDirector> current = director;
        if (current.isEmpty()) {
            return;
        }
        long began = System.nanoTime();
        current.get().periodic();
        counters.hologramWork(System.nanoTime() - began);
    }

    @Override
    public Optional<NametagDirector> get() {
        return director;
    }
}
