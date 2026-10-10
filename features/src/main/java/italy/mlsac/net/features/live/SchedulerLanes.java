// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.alerting.DeliveryLane;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.scoring.PlayerThread;
import java.util.UUID;

public final class SchedulerLanes implements DeliveryLane, PlayerThread {

    private final TaskScheduler scheduler;
    private final PlayerDirectory players;

    public SchedulerLanes(TaskScheduler scheduler, PlayerDirectory players) {
        this.scheduler = scheduler;
        this.players = players;
    }

    @Override
    public void onPlayerThread(UUID player, Runnable task) {
        scheduler.submit(TaskLane.following(player), TaskTiming.immediately(), task);
    }

    @Override
    public void onMainThread(Runnable task) {
        scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), task);
    }

    @Override
    public boolean online(UUID player) {
        return players.byId(player).map(PlayerView::online).orElse(false);
    }

    @Override
    public boolean runNextTick(UUID player, Runnable task) {
        if (!online(player)) {
            return false;
        }
        onPlayerThread(player, task);
        return true;
    }
}
