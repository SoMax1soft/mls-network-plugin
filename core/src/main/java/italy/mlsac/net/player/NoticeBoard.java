// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class NoticeBoard {

    private final PlayerDirectory players;
    private final TaskScheduler scheduler;

    public NoticeBoard(PlayerDirectory players, TaskScheduler scheduler) {
        this.players = players;
        this.scheduler = scheduler;
    }

    public void broadcast(Predicate<PlayerView> audience, List<String> coloredLines) {
        scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), () -> {
            for (PlayerView player : players.online()) {
                UUID id = player.id();
                scheduler.submit(TaskLane.following(id), TaskTiming.immediately(), () -> deliver(id, audience, coloredLines));
            }
        });
    }

    public void sendLater(UUID player, Predicate<PlayerView> audience, List<String> coloredLines, long delayTicks) {
        scheduler.submit(TaskLane.following(player), TaskTiming.after(delayTicks), () -> deliver(player, audience, coloredLines));
    }

    private void deliver(UUID player, Predicate<PlayerView> audience, List<String> lines) {
        players.byId(player).filter(PlayerView::online).filter(audience)
                .ifPresent(view -> lines.forEach(view::sendMessage));
    }
}
