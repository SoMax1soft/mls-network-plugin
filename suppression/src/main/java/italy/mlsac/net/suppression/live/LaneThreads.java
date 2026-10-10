// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.suppression.server.ServerThreads;
import java.util.UUID;

public final class LaneThreads implements ServerThreads {

    private final TaskScheduler scheduler;
    private final PlayerDirectory players;

    public LaneThreads(TaskScheduler scheduler, PlayerDirectory players) {
        this.scheduler = scheduler;
        this.players = players;
    }

    @Override
    public void onSuspectThread(UUID suspectId, Runnable whileOnline, Runnable whenOffline) {
        boolean online = players.byId(suspectId).map(PlayerView::online).orElse(false);
        if (!online) {
            onGlobalThread(whenOffline);
            return;
        }
        scheduler.submit(TaskLane.following(suspectId, () -> onGlobalThread(whenOffline)), TaskTiming.immediately(),
                whileOnline);
    }

    @Override
    public void onGlobalThread(Runnable task) {
        scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), task);
    }
}
