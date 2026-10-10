// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.lifecycle;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;

public final class RepeatingTickSource implements TickSource {

    private final TaskScheduler scheduler;
    private final EventBus bus;
    private TaskTicket task;
    private long tick;

    public RepeatingTickSource(TaskScheduler scheduler, EventBus bus) {
        this.scheduler = scheduler;
        this.bus = bus;
    }

    @Override
    public void start() {
        tick = 0;
        task = scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(1L, 1L), () -> bus.publish(new ServerTick(++tick)));
    }

    @Override
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
