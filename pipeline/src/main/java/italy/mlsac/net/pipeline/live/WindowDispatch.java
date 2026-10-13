// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.backend.BackendVerdict;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.detection.ModelVerdict;
import italy.mlsac.net.detection.WindowKind;
import italy.mlsac.net.detection.WindowSubmitted;
import italy.mlsac.net.pipeline.environment.LegacyWindowSink;
import italy.mlsac.net.pipeline.environment.UltraWindowSink;
import italy.mlsac.net.pipeline.environment.WindowEncoding;
import italy.mlsac.net.workload.WorkloadCounters;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class WindowDispatch implements UltraWindowSink {

    private static final String ULTRA_MODEL = "ultra";

    private final BackendGateway backend;
    private final EventBus bus;
    private final Function<UUID, String> names;
    private final PluginLog log;
    private final WorkloadCounters counters;
    private final LongSupplier nanos;
    private final AtomicLong tickets = new AtomicLong();
    private final LegacyWindowSink legacy = new LegacyWindowSink() {
        @Override
        public void accept(UUID player, byte[] window) {
            submit(player, window.clone(), WindowKind.LEGACY);
        }

        @Override
        public void encodeAndAccept(UUID player, WindowEncoding encoding) {
            submit(player, encodeMetered(encoding), WindowKind.LEGACY);
        }
    };

    public WindowDispatch(BackendGateway backend, EventBus bus, Function<UUID, String> names, PluginLog log,
                          WorkloadCounters counters, LongSupplier nanos) {
        this.backend = backend;
        this.bus = bus;
        this.names = names;
        this.log = log;
        this.counters = counters;
        this.nanos = nanos;
    }

    @Override
    public void accept(UUID player, byte[] window) {
        submit(player, window.clone(), WindowKind.ULTRA);
    }

    @Override
    public void encodeAndAccept(UUID player, WindowEncoding encoding) {
        submit(player, encodeMetered(encoding), WindowKind.ULTRA);
    }

    public LegacyWindowSink legacy() {
        return legacy;
    }

    private byte[] encodeMetered(WindowEncoding encoding) {
        long began = nanos.getAsLong();
        byte[] window = encoding.encode();
        counters.payloadBuilt(nanos.getAsLong() - began, window.length);
        return window;
    }

    private void submit(UUID player, byte[] window, WindowKind kind) {
        long ticket = tickets.incrementAndGet();
        String nick = names.apply(player);
        bus.publish(new WindowSubmitted(player, nick, kind, ticket));
        DebugTally.count("backend.sent." + kind.name().toLowerCase(Locale.ROOT));
        DebugTally.trace(player, () -> kind + " window sent (" + window.length + " bytes)");
        backend.predict(WindowSubmission.handedOver(player, nick, window),
                verdict -> answered(player, nick, kind, ticket, verdict))
                .whenComplete((outcome, failure) -> settled(player, nick, kind, outcome, failure));
    }

    private void answered(UUID player, String nick, WindowKind kind, long ticket, BackendVerdict verdict) {
        if (kind == WindowKind.ULTRA && !Double.isFinite(verdict.probability())) {
            return;
        }
        String model = kind == WindowKind.ULTRA ? ULTRA_MODEL : verdict.model();
        DebugTally.count("backend.answer." + model);
        DebugTally.trace(player, () -> "answer " + model + " " + String.format(Locale.ROOT, "%.3f", verdict.probability()));
        long began = nanos.getAsLong();
        try {
            bus.publish(new ModelVerdict(player, nick, model, verdict.probability(), kind, ticket));
        } finally {
            counters.responseHandled(nanos.getAsLong() - began);
        }
    }

    private void settled(UUID player, String nick, WindowKind kind, PredictOutcome outcome, Throwable failure) {
        if (failure != null) {
            DebugTally.count("backend.result.failed");
            DebugTally.trace(player, () -> kind + " window failed: " + failure);
        } else if (outcome instanceof PredictOutcome.Completed) {
            DebugTally.count("backend.result.completed");
        } else {
            DebugTally.count("backend.result." + String.valueOf(outcome).replace(' ', '_'));
            DebugTally.trace(player, () -> kind + " window ended as " + outcome);
        }
        if (failure != null) {
            log.debug(LogTopic.DETECTION, () -> "Prediction for " + nick + " (" + kind + ") failed: " + failure);
            return;
        }
        if (!(outcome instanceof PredictOutcome.Completed)) {
            log.debug(LogTopic.DETECTION, () -> "Prediction for " + nick + " (" + kind + ") ended as " + outcome);
        }
    }
}
