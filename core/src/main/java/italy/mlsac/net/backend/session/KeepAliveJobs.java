// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;
import java.util.EnumMap;
import java.util.Map;

final class KeepAliveJobs {

    enum Job {
        HEARTBEAT(100, 600),
        ONLINE(100, 600),
        POLL(60, 60),
        CHECK(100, 1200),
        STASIS_PROBE(100, 6000);

        private final long delayTicks;
        private final long everyTicks;

        Job(long delayTicks, long everyTicks) {
            this.delayTicks = delayTicks;
            this.everyTicks = everyTicks;
        }
    }

    private final TaskScheduler scheduler;
    private final Map<Job, Runnable> actions = new EnumMap<>(Job.class);
    private final Map<Job, TaskTicket> running = new EnumMap<>(Job.class);
    private boolean closed;

    KeepAliveJobs(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    synchronized void bind(Job job, Runnable action) {
        actions.put(job, action);
    }

    synchronized void start(Job... jobs) {
        if (closed) {
            return;
        }
        for (Job job : jobs) {
            cancelOne(job);
            Runnable action = actions.get(job);
            if (action != null) {
                running.put(job, scheduler.submit(TaskLane.BACKGROUND, TaskTiming.repeating(job.delayTicks, job.everyTicks), action));
            }
        }
    }

    synchronized void cancel(Job... jobs) {
        for (Job job : jobs) {
            cancelOne(job);
        }
    }

    synchronized void close() {
        closed = true;
        running.values().forEach(TaskTicket::cancel);
        running.clear();
    }

    private void cancelOne(Job job) {
        TaskTicket task = running.remove(job);
        if (task != null) {
            task.cancel();
        }
    }
}
