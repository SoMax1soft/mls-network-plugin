// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class OwningThread {

    private final TaskScheduler scheduler;
    private final boolean folia;
    private final PluginLog log;

    public OwningThread(TaskScheduler scheduler, boolean folia, PluginLog log) {
        this.scheduler = scheduler;
        this.folia = folia;
        this.log = log;
    }

    public boolean folia() {
        return folia;
    }

    public void withPlayer(UUID id, Consumer<Player> action) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return;
        }
        if (owns(player)) {
            action.accept(player);
            return;
        }
        laterWithPlayer(id, TaskTiming.immediately(), action);
    }

    public void laterWithPlayer(UUID id, TaskTiming timing, Consumer<Player> action) {
        submit(TaskLane.following(id), timing, () -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                action.accept(player);
            }
        });
    }

    public void atLocation(Location place, Runnable action) {
        World world = place.getWorld();
        if (world == null) {
            return;
        }
        if (ownsLocation(place)) {
            action.run();
            return;
        }
        submit(TaskLane.chunkAt(world.getUID(), place.getBlockX(), place.getBlockZ()), TaskTiming.immediately(),
                action);
    }

    public void onGlobal(Runnable action) {
        if (!folia && scheduler.onGlobalThread()) {
            action.run();
            return;
        }
        submit(TaskLane.MAIN, TaskTiming.immediately(), action);
    }

    public boolean owns(Player player) {
        return folia ? Bukkit.isOwnedByCurrentRegion(player) : Bukkit.isPrimaryThread();
    }

    private boolean ownsLocation(Location place) {
        return folia ? Bukkit.isOwnedByCurrentRegion(place) : Bukkit.isPrimaryThread();
    }

    private void submit(TaskLane lane, TaskTiming timing, Runnable action) {
        try {
            scheduler.submit(lane, timing, action);
        } catch (RuntimeException refused) {
            log.warn(LogTopic.GENERAL, "A server task could not be scheduled: " + refused.getMessage());
        }
    }
}
