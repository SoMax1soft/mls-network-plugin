// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule.bukkit;

import italy.mlsac.net.schedule.TaskTicket;
import java.util.UUID;
import org.bukkit.Bukkit;

final class PlayerBoundRepeat implements Runnable {

    private final UUID player;
    private final Runnable task;
    private final Runnable onRetired;
    private volatile TaskTicket handle;

    PlayerBoundRepeat(UUID player, Runnable task, Runnable onRetired) {
        this.player = player;
        this.task = task;
        this.onRetired = onRetired;
    }

    void bind(TaskTicket handle) {
        this.handle = handle;
    }

    static void whileOnline(UUID player, Runnable task, Runnable whenGone) {
        if (Bukkit.getPlayer(player) != null) {
            task.run();
        } else {
            whenGone.run();
        }
    }

    @Override
    public void run() {
        if (Bukkit.getPlayer(player) != null) {
            task.run();
            return;
        }
        TaskTicket bound = handle;
        if (bound != null) {
            bound.cancel();
        }
        onRetired.run();
    }
}
