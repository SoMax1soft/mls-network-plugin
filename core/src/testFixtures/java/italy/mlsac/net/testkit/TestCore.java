// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.config.ConfigApplied;
import italy.mlsac.net.config.ConfigChangeCause;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.lifecycle.ReloadOutcome;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.HostHandles;
import italy.mlsac.net.platform.ClassProbe;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.platform.ServerVersionRules;
import italy.mlsac.net.player.BedrockDetector;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.player.OnlinePlayerCount;
import italy.mlsac.net.player.StandingCache;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.region.RegionLookup;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.workload.WorkloadCounters;
import italy.mlsac.net.yaml.SnakeYamlParser;
import java.util.concurrent.CompletableFuture;

public final class TestCore {

    public final ManualScheduler scheduler = new ManualScheduler();
    public final ManualClock clock = new ManualClock(1_789_000_000_000L);
    public final ScriptedRandom random = new ScriptedRandom(0);
    public final CapturingLogSink logSink = new CapturingLogSink();
    public final ConsoleLog log = TestLogs.capturing(logSink, clock, false);
    public final SynchronousEventBus bus = new SynchronousEventBus(log);
    public final ConfigReference config = new ConfigReference(TestConfigs.defaults());
    public final MessageBook messages = TestConfigs.messages(config);
    public final FakeBackend backend = new FakeBackend();
    public final FakePacketLayer packets = new FakePacketLayer(GameVersion.of(1, 21, 4));
    public final FakePlayers players = new FakePlayers();
    public final OnlinePlayerCount online = new OnlinePlayerCount();
    public final StandingCache standings = new StandingCache(BedrockDetector.NONE);
    public final MemoryDataFolder storageRoot = new MemoryDataFolder();
    public final RegionExemptions regions = new RegionExemptions(() -> config.get().detection().regions(),
            RegionLookup.NONE, log);
    public final WorkloadCounters workload = new WorkloadCounters();

    public CoreServices services() {
        PlatformProfile platform = PlatformProfile.detect(ClassProbe.of(getClass().getClassLoader()),
                "1.21.4-R0.1-SNAPSHOT", 21);
        return new CoreServices("2.0.dev", platform, new ServerVersionRules(packets.serverVersion()), scheduler,
                clock, random, log, bus, config, messages, backend, packets, players,
                new NoticeBoard(players, scheduler), standings, regions, online, storageRoot,
                new ClasspathResources(getClass().getClassLoader()), new SnakeYamlParser(),
                kind -> CompletableFuture.completedFuture(ReloadOutcome.succeeded()), HostHandles.detached(), workload);
    }

    public void apply(EffectiveConfig next) {
        EffectiveConfig previous = config.get();
        config.publish(next);
        bus.publish(new ConfigApplied(previous, next, ConfigChangeCause.PRESET));
    }
}
