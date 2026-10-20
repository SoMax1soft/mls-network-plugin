// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice.bukkit;

import italy.mlsac.net.backend.http.WorkerPool;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.concurrent.RejectedExecutionException;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;

final class ServerEventSink implements ApiEventSink, AutoCloseable {

    private final PluginManager plugins;
    private final PluginLog log;
    private final WorkerPool lane = new WorkerPool("api-events", 1);

    ServerEventSink(PluginManager plugins, PluginLog log) {
        this.plugins = plugins;
        this.log = log;
    }

    @Override
    public void fire(Event event) {
        try {
            lane.executor().execute(() -> deliver(event));
        } catch (RejectedExecutionException stopping) {
            log.debug(LogTopic.DETECTION, () -> "API event dropped while stopping: " + event.getEventName());
        }
    }

    @Override
    public void close() {
        lane.close();
    }

    private void deliver(Event event) {
        try {
            plugins.callEvent(event);
        } catch (RuntimeException failure) {
            log.warn(LogTopic.DETECTION, "A listener of " + event.getEventName() + " failed: " + failure, failure);
        }
    }
}
