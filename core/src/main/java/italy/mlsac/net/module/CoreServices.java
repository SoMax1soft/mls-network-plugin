// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.module;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.lifecycle.ConfigReloader;
import italy.mlsac.net.packet.PacketLayer;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.platform.ServerVersionRules;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.player.OnlinePlayerCount;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.StandingCache;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.resource.ResourceSource;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.storage.DataFolder;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.workload.WorkloadCounters;
import italy.mlsac.net.yaml.YamlParser;

public record CoreServices(
        String buildVersion,
        PlatformProfile platform,
        ServerVersionRules serverRules,
        TaskScheduler scheduler,
        Clock clock,
        RandomSource random,
        PluginLog log,
        EventBus bus,
        ConfigReference config,
        MessageBook messages,
        BackendGateway backend,
        PacketLayer packets,
        PlayerDirectory players,
        NoticeBoard notices,
        StandingCache standings,
        RegionExemptions regions,
        OnlinePlayerCount online,
        DataFolder storageRoot,
        ResourceSource resources,
        YamlParser yaml,
        ConfigReloader reloader,
        HostHandles host,
        WorkloadCounters workload) {
}
