// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule.bukkit;

import italy.mlsac.net.schedule.LaneRoutes;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitTaskScheduler implements TaskScheduler {

    private final Plugin plugin;
    private final BukkitTickTiming timing;

    public BukkitTaskScheduler(Plugin plugin, BukkitTickTiming timing) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    @Override
    public TaskTicket submit(TaskLane lane, TaskTiming when, Runnable task) {
        return lane.route(new LaneRoutes<>() {
            @Override
            public TaskTicket main() {
                return onServerThread(when, task);
            }

            @Override
            public TaskTicket background() {
                return offServerThread(when, task);
            }

            @Override
            public TaskTicket player(UUID player, Runnable whenGone) {
                return followPlayer(player, whenGone, when, task);
            }

            @Override
            public TaskTicket chunk(UUID world, int blockX, int blockZ) {
                return onServerThread(when, task);
            }
        });
    }

    @Override
    public boolean onGlobalThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void revokeEvery() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    private TaskTicket onServerThread(TaskTiming when, Runnable task) {
        BukkitTask handle = switch (when.cadence()) {
            case NOW -> Bukkit.getScheduler().runTask(plugin, task);
            case ONCE_LATER -> Bukkit.getScheduler().runTaskLater(plugin, task, timing.delayTicks(when.delayTicks()));
            case REPEATING -> Bukkit.getScheduler().runTaskTimer(plugin, task, timing.delayTicks(when.delayTicks()),
                    timing.everyTicks(when.everyTicks()));
        };
        return new BukkitTicket(handle);
    }

    private TaskTicket offServerThread(TaskTiming when, Runnable task) {
        BukkitTask handle = switch (when.cadence()) {
            case NOW -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            case ONCE_LATER -> Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task,
                    timing.delayTicks(when.delayTicks()));
            case REPEATING -> Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task,
                    timing.delayTicks(when.delayTicks()), timing.everyTicks(when.everyTicks()));
        };
        return new BukkitTicket(handle);
    }

    private TaskTicket followPlayer(UUID player, Runnable whenGone, TaskTiming when, Runnable task) {
        if (!when.repeats()) {
            return onServerThread(when, () -> PlayerBoundRepeat.whileOnline(player, task, whenGone));
        }
        PlayerBoundRepeat repeat = new PlayerBoundRepeat(player, task, whenGone);
        TaskTicket handle = onServerThread(when, repeat);
        repeat.bind(handle);
        return handle;
    }

    private record BukkitTicket(BukkitTask task) implements TaskTicket {
        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
