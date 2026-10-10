// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import italy.mlsac.net.schedule.InertTask;
import italy.mlsac.net.schedule.LaneRoutes;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FoliaTaskScheduler implements TaskScheduler {

    private final Plugin plugin;
    private final FoliaTickTiming timing;
    private final Set<TaskTicket> playerRepeats = ConcurrentHashMap.newKeySet();

    public FoliaTaskScheduler(Plugin plugin, FoliaTickTiming timing) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    @Override
    public TaskTicket submit(TaskLane lane, TaskTiming when, Runnable task) {
        Consumer<ScheduledTask> body = ignored -> task.run();
        return lane.route(new LaneRoutes<>() {
            @Override
            public TaskTicket main() {
                return globalRegion(when, body);
            }

            @Override
            public TaskTicket background() {
                return asyncPool(when, body);
            }

            @Override
            public TaskTicket player(UUID player, Runnable whenGone) {
                return entityThread(player, whenGone, when, body);
            }

            @Override
            public TaskTicket chunk(UUID world, int blockX, int blockZ) {
                return regionThread(world, blockX, blockZ, when, body);
            }
        });
    }

    @Override
    public boolean onGlobalThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void revokeEvery() {
        playerRepeats.forEach(TaskTicket::cancel);
        playerRepeats.clear();
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }

    private TaskTicket globalRegion(TaskTiming when, Consumer<ScheduledTask> body) {
        var region = Bukkit.getGlobalRegionScheduler();
        return ticket(switch (when.cadence()) {
            case NOW -> region.run(plugin, body);
            case ONCE_LATER -> region.runDelayed(plugin, body, timing.delayTicks(when.delayTicks()));
            case REPEATING -> region.runAtFixedRate(plugin, body, timing.delayTicks(when.delayTicks()),
                    timing.everyTicks(when.everyTicks()));
        });
    }

    private TaskTicket asyncPool(TaskTiming when, Consumer<ScheduledTask> body) {
        var pool = Bukkit.getAsyncScheduler();
        return ticket(switch (when.cadence()) {
            case NOW -> pool.runNow(plugin, body);
            case ONCE_LATER -> pool.runDelayed(plugin, body, timing.asyncMillis(when.delayTicks()),
                    TimeUnit.MILLISECONDS);
            case REPEATING -> pool.runAtFixedRate(plugin, body, timing.asyncMillis(when.delayTicks()),
                    timing.asyncMillis(when.everyTicks()), TimeUnit.MILLISECONDS);
        });
    }

    private TaskTicket entityThread(UUID player, Runnable whenGone, TaskTiming when, Consumer<ScheduledTask> body) {
        Player target = Bukkit.getPlayer(player);
        if (target == null || !target.isValid()) {
            return new InertTask();
        }
        var entity = target.getScheduler();
        TaskTicket handle = ticket(switch (when.cadence()) {
            case NOW -> entity.run(plugin, body, whenGone);
            case ONCE_LATER -> entity.runDelayed(plugin, body, whenGone, timing.delayTicks(when.delayTicks()));
            case REPEATING -> entity.runAtFixedRate(plugin, body, whenGone, timing.delayTicks(when.delayTicks()),
                    timing.everyTicks(when.everyTicks()));
        });
        if (when.repeats()) {
            playerRepeats.removeIf(TaskTicket::isCancelled);
            playerRepeats.add(handle);
        }
        return handle;
    }

    private TaskTicket regionThread(UUID world, int blockX, int blockZ, TaskTiming when,
            Consumer<ScheduledTask> body) {
        World target = world == null ? null : Bukkit.getWorld(world);
        if (target == null) {
            return new InertTask();
        }
        var region = Bukkit.getRegionScheduler();
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        return ticket(switch (when.cadence()) {
            case NOW -> region.run(plugin, target, chunkX, chunkZ, body);
            case ONCE_LATER -> region.runDelayed(plugin, target, chunkX, chunkZ, body,
                    timing.delayTicks(when.delayTicks()));
            case REPEATING -> region.runAtFixedRate(plugin, target, chunkX, chunkZ, body,
                    timing.delayTicks(when.delayTicks()), timing.everyTicks(when.everyTicks()));
        });
    }

    private static TaskTicket ticket(ScheduledTask task) {
        return task == null ? new InertTask() : new FoliaTicket(task);
    }

    private record FoliaTicket(ScheduledTask task) implements TaskTicket {
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
