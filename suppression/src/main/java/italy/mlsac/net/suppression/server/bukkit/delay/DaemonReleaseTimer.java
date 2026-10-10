// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.delay;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class DaemonReleaseTimer implements ReleaseTimer {

    private static final String THREAD_NAME = "MLSAC-suppression-release";

    private final ScheduledThreadPoolExecutor executor;
    private final PluginLog log;

    public DaemonReleaseTimer(PluginLog log) {
        this.log = log;
        this.executor = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    @Override
    public void after(long delayMillis, Runnable task) {
        try {
            executor.schedule(() -> runGuarded(task), Math.max(0L, delayMillis), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException closed) {
            log.debug(LogTopic.SUPPRESSION, () -> "held packet release refused after shutdown");
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void runGuarded(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException failure) {
            log.error(LogTopic.SUPPRESSION, "held packet release failed", failure);
        }
    }
}
