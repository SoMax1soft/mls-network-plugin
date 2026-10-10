// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.suppression.server.bukkit.cadence.OwnTeleports;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public final class RegionTeleport implements TeleportRoute {

    private final OwnTeleports ownTeleports;
    private final TaskScheduler scheduler;
    private final PluginLog log;

    public RegionTeleport(OwnTeleports ownTeleports, TaskScheduler scheduler, PluginLog log) {
        this.ownTeleports = ownTeleports;
        this.scheduler = scheduler;
        this.log = log;
    }

    @Override
    public void travel(Player player, Location target, Runnable arrived) {
        UUID suspectId = player.getUniqueId();
        ownTeleports.begin(suspectId);
        player.teleportAsync(target, TeleportCause.PLUGIN)
                .whenComplete((moved, failure) -> settled(suspectId, Boolean.TRUE.equals(moved), failure, arrived));
    }

    private void settled(UUID suspectId, boolean moved, Throwable failure, Runnable arrived) {
        ownTeleports.finish(suspectId);
        if (failure != null) {
            log.debug(LogTopic.SUPPRESSION, () -> "engine teleport failed: " + failure);
        } else if (moved) {
            scheduler.submit(TaskLane.following(suspectId), TaskTiming.immediately(), arrived);
        }
    }
}
