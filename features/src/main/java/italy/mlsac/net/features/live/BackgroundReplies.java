// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.commands.CommandLanes;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.List;

public final class BackgroundReplies implements CommandLanes {

    private final TaskScheduler scheduler;

    public BackgroundReplies(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void inBackground(Runnable task) {
        scheduler.submit(TaskLane.BACKGROUND, TaskTiming.immediately(), task);
    }

    @Override
    public void reply(CommandActor actor, List<String> lines) {
        TaskLane lane = actor.actorUuid().map(TaskLane::following).orElse(TaskLane.MAIN);
        scheduler.submit(lane, TaskTiming.immediately(), () -> lines.forEach(actor::send));
    }
}
