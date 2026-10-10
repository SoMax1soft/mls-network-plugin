// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.update;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.http.HttpHeader;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.http.HttpTransport;
import italy.mlsac.net.backend.http.TransportFailure;
import italy.mlsac.net.backend.session.LenientJson;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public final class ReleaseWatcher {

    public static final long CADENCE_TICKS = 6000L;
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(45);
    private static final String API_ROOT = "/api/v1";

    private final TaskScheduler scheduler;
    private final HttpTransport transport;
    private final PluginLog log;
    private final String endpoint;
    private final String buildVersion;
    private final int javaChannel;
    private final BooleanSupplier enabled;
    private final AtomicBoolean unanswered = new AtomicBoolean();
    private volatile Optional<String> available = Optional.empty();
    private volatile String announced = "";
    private TaskTicket timer;

    public ReleaseWatcher(TaskScheduler scheduler, HttpTransport transport, PluginLog log, String endpoint,
                         String buildVersion, int javaChannel, BooleanSupplier enabled) {
        this.scheduler = scheduler;
        this.transport = transport;
        this.log = log;
        this.endpoint = endpoint;
        this.buildVersion = buildVersion;
        this.javaChannel = javaChannel;
        this.enabled = enabled;
    }

    public synchronized void start() {
        if (buildVersion.toLowerCase(Locale.ROOT).contains("dev")) {
            return;
        }
        String base = base();
        if (!base.toLowerCase(Locale.ROOT).startsWith("https://")) {
            log.warn(LogTopic.UPDATER, "Update checks are off: " + base + " is not an https:// address");
            return;
        }
        timer = scheduler.submit(TaskLane.BACKGROUND, TaskTiming.repeating(0L, CADENCE_TICKS), this::checkOnce);
    }

    public synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public Optional<String> availableVersion() {
        return available;
    }

    public URI requestUri() {
        return URI.create(base() + "/plugin/update?java=" + javaChannel + "&version="
                + URLEncoder.encode(buildVersion, StandardCharsets.UTF_8));
    }

    void checkOnce() {
        if (!enabled.getAsBoolean() || !unanswered.compareAndSet(false, true)) {
            return;
        }
        List<HttpHeader> headers = List.of(new HttpHeader("User-Agent", "MLSAC-Plugin-Updater/" + buildVersion),
                new HttpHeader("Accept", "application/json"));
        transport.send(HttpCall.get(requestUri(), headers, READ_TIMEOUT, Integer.MAX_VALUE))
                .whenComplete((reply, error) -> {
                    try {
                        if (error != null) {
                            log.warn(LogTopic.UPDATER, "Failed to check for update: "
                                    + TransportFailure.of(error).kind());
                        } else {
                            read(reply);
                        }
                    } finally {
                        unanswered.set(false);
                    }
                });
    }

    private void read(HttpReply reply) {
        if (reply.status() != 200) {
            available = Optional.empty();
            return;
        }
        Optional<JsonObject> data = LenientJson.object(reply.text())
                .filter(root -> root.bool("success").orElse(false))
                .flatMap(root -> root.object("data"));
        Optional<String> version = data.filter(d -> d.bool("updateAvailable").orElse(false))
                .flatMap(d -> d.string("version"));
        available = version;
        version.filter(v -> !v.equals(announced)).ifPresent(v -> {
            announced = v;
            log.warn(LogTopic.UPDATER, "MLSAC " + v + " is available");
        });
    }

    private String base() {
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.endsWith(API_ROOT) ? trimmed : trimmed + API_ROOT;
    }
}
