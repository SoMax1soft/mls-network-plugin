// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.status;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.backend.LatencyResult;
import italy.mlsac.net.backend.TodayStats;
import italy.mlsac.net.backend.session.ApiState;
import italy.mlsac.net.backend.session.BackendStatus;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class StatusSubcommand implements Subcommand {

    private static final String UNKNOWN_COUNT = "-";

    private final BackendGateway backend;
    private final Supplier<EffectiveConfig> config;
    private final MessageBook messages;
    private final TaskScheduler scheduler;

    public StatusSubcommand(BackendGateway backend, Supplier<EffectiveConfig> config, MessageBook messages,
                            TaskScheduler scheduler) {
        this.backend = backend;
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.STATUS;
    }

    @Override
    public void execute(CommandActor actor, List<String> args) {
        BackendStatus status = backend.status();
        actor.send(line("status-title", TemplateValues.none()));
        actor.send(line("status-link", TemplateValues.of("{STATE}", messages.rawText(stateKey(status.state())))));
        actor.send(line("status-address", TemplateValues.of("{ADDRESS}", status.currentBase().orElse("N/A"))));
        actor.send(presetLine(config.get()));
        boolean probing = status.latencyProbeAllowed();
        CompletableFuture<Optional<TodayStats>> counters = backend.todayStats().exceptionally(error -> Optional.empty());
        CompletableFuture<String> latency = probing
                ? backend.probeLatency().handle((result, error) -> error == null ? latencyLine(result)
                        : line("status-latency-error", TemplateValues.none()))
                : CompletableFuture.completedFuture(line("status-latency-unavailable", TemplateValues.none()));
        counters.thenCombine(latency, (today, roundTrip) -> List.of(
                        countLine("status-today-detections", today, TodayStats::detections),
                        countLine("status-today-requests", today, TodayStats::requestsUsed),
                        roundTrip))
                .thenAccept(lines -> deliver(actor, lines));
    }

    private String countLine(String key, Optional<TodayStats> today, Function<TodayStats, Long> counter) {
        String shown = today.map(counter).map(String::valueOf).orElse(UNKNOWN_COUNT);
        return line(key, TemplateValues.of("{COUNT}", shown));
    }

    private String latencyLine(LatencyResult result) {
        if (result.averageMillis().isEmpty()) {
            return line("status-latency-none", TemplateValues.none());
        }
        long millis = result.averageMillis().getAsLong();
        String speed = millis >= 150 ? "&c" : millis >= 50 ? "&e" : "&a";
        return line("status-latency", TemplateValues.of("{SPEED}", speed).and("{MS}", millis)
                .and("{OK}", result.successes()).and("{TOTAL}", result.attempts()));
    }

    private String presetLine(EffectiveConfig current) {
        String configured = current.core().preset().configuredName();
        if (current.preset().applied()) {
            String active = current.preset().resolvedName().orElse(configured);
            return line("status-preset-applied", TemplateValues.of("{PRESET}", active));
        }
        return configured.isEmpty() ? line("status-preset-auto", TemplateValues.none())
                : line("status-preset-pending", TemplateValues.of("{PRESET}", configured));
    }

    private static String stateKey(ApiState state) {
        return switch (state) {
            case NOT_INITIALIZED -> "status-state-none";
            case STASIS -> "status-state-stasis";
            case CONNECTED -> "status-state-online";
            case RATE_LIMITED -> "status-state-limited";
            case BACKEND_FAULT -> "status-state-fault";
            case DISCONNECTED -> "status-state-offline";
        };
    }

    private String line(String key, TemplateValues values) {
        return messages.text(key, values);
    }

    private void deliver(CommandActor actor, List<String> lines) {
        Runnable send = () -> lines.forEach(actor::send);
        TaskLane lane = actor.actorUuid().map(TaskLane::following).orElse(TaskLane.MAIN);
        scheduler.submit(lane, TaskTiming.immediately(), send);
    }
}
