// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskScheduler;
import java.util.UUID;

public final class StandingRefresher {

    private final TaskScheduler scheduler;
    private final PlayerDirectory players;
    private final StandingCache standings;
    private final PermissionChanges changes;
    private TaskTicket task;
    private PermissionChanges.Closing listening;

    public StandingRefresher(TaskScheduler scheduler, PlayerDirectory players, StandingCache standings) {
        this(scheduler, players, standings, PermissionChanges.NONE);
    }

    public StandingRefresher(TaskScheduler scheduler, PlayerDirectory players, StandingCache standings,
                             PermissionChanges changes) {
        this.scheduler = scheduler;
        this.players = players;
        this.standings = standings;
        this.changes = changes;
    }

    public void start() {
        task = scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(StandingCache.RESAMPLE_TICKS, StandingCache.RESAMPLE_TICKS), this::refreshAll);
        listening = changes.listen(this::refreshOne);
        refreshAll();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (listening != null) {
            listening.close();
            listening = null;
        }
        standings.clear();
    }

    private void refreshAll() {
        for (PlayerView player : players.online()) {
            refreshOne(player.id());
        }
    }

    private void refreshOne(UUID player) {
        scheduler.submit(TaskLane.following(player), TaskTiming.immediately(),
                () -> players.byId(player).ifPresent(standings::refresh));
    }
}
