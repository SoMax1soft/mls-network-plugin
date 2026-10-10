// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.module.HostHandles;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.suppression.time.MonotonicClock;

public record SuppressionHostParts(PlayerDirectory players, RegionExemptions regions, TaskScheduler scheduler,
                                   PacketLayer packets, MonotonicClock clock, EventBus bus, PluginLog log,
                                   PlatformProfile platform, HostHandles host) {
}
