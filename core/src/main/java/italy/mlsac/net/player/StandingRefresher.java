// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;

public final class StandingRefresher {

    private final TaskScheduler scheduler;
    private final PlayerDirectory players;
    private final StandingCache standings;
    private TaskTicket task;

    public StandingRefresher(TaskScheduler scheduler, PlayerDirectory players, StandingCache standings) {
        this.scheduler = scheduler;
        this.players = players;
        this.standings = standings;
    }

    public void start() {
        task = scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(StandingCache.RESAMPLE_TICKS, StandingCache.RESAMPLE_TICKS), this::refreshAll);
        refreshAll();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        standings.clear();
    }

    private void refreshAll() {
        for (PlayerView player : players.online()) {
            scheduler.submit(TaskLane.following(player.id()), TaskTiming.immediately(), () -> players.byId(player.id()).ifPresent(standings::refresh));
        }
    }
}
