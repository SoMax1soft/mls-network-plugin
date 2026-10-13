// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.backend.BackendAddress;
import italy.mlsac.net.backend.BackendService;
import italy.mlsac.net.backend.LoopbackOverride;
import italy.mlsac.net.backend.http.FreshClients;
import italy.mlsac.net.backend.http.JdkHttpTransport;
import italy.mlsac.net.backend.http.WorkerPool;
import italy.mlsac.net.backend.notice.SharedNameAlarm;
import italy.mlsac.net.backend.notice.InterServerFeed;
import italy.mlsac.net.backend.notice.ReplyObservers;
import italy.mlsac.net.backend.request.ServerAddress;
import italy.mlsac.net.backend.session.JdkSessionSource;
import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.clock.RandomSource;
import italy.mlsac.net.clock.SystemClock;
import italy.mlsac.net.clock.SystemRandomSource;
import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigComposer;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.core.CoreSettingsReader;
import italy.mlsac.net.config.local.LocalConfigFile;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.console.SecretRedactor;
import italy.mlsac.net.console.jul.JulLogSink;
import italy.mlsac.net.lifecycle.JoinNotices;
import italy.mlsac.net.lifecycle.PerPlayerTickSource;
import italy.mlsac.net.lifecycle.RepeatingTickSource;
import italy.mlsac.net.lifecycle.TickSource;
import italy.mlsac.net.lifecycle.bukkit.PaperTickEndSource;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.player.BedrockDetector;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.player.OnlinePlayerCount;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.StandingCache;
import italy.mlsac.net.player.bukkit.InstalledPermissionPlugin;
import italy.mlsac.net.player.StandingRefresher;
import italy.mlsac.net.player.bukkit.BukkitPlayerDirectory;
import italy.mlsac.net.player.bukkit.BukkitPlayerEventBridge;
import italy.mlsac.net.player.bukkit.ReflectiveBedrockDetector;
import italy.mlsac.net.preset.MissingPresetNotices;
import italy.mlsac.net.preset.PresetFetcher;
import italy.mlsac.net.preset.PresetPoller;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.region.RegionLookup;
import italy.mlsac.net.region.worldguard.WorldGuardRegionLookup;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.resource.ResourceSource;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.bukkit.BukkitTaskScheduler;
import italy.mlsac.net.schedule.bukkit.BukkitTickTiming;
import italy.mlsac.net.schedule.folia.FoliaTaskScheduler;
import italy.mlsac.net.schedule.folia.FoliaTickTiming;
import italy.mlsac.net.storage.DataFolder;
import italy.mlsac.net.storage.DiskDataFolder;
import italy.mlsac.net.telemetry.ClientCensus;
import italy.mlsac.net.telemetry.ServerEnvironment;
import italy.mlsac.net.telemetry.bukkit.BukkitServerFacts;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.MessageCatalog;
import italy.mlsac.net.update.ReleaseWatcher;
import italy.mlsac.net.workload.WorkloadCounters;
import italy.mlsac.net.yaml.SnakeYamlParser;
import italy.mlsac.net.yaml.YamlFormatException;
import italy.mlsac.net.yaml.YamlParser;
import java.time.Duration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

final class CoreWiring {

    final JavaPlugin plugin;
    final PlatformProfile platform;
    final String version;
    final BackendAddress address;
    final Clock clock = new SystemClock();
    final RandomSource random = new SystemRandomSource();
    final SecretRedactor redactor = new SecretRedactor();
    final ConfigReference config;
    final ConsoleLog log;
    final SynchronousEventBus bus;
    final TaskScheduler scheduler;
    final ResourceSource resources;
    final YamlParser yaml = new SnakeYamlParser();
    final DataFolder storageRoot;
    final LocalConfigFile localFile;
    final CoreSettingsReader coreReader = new CoreSettingsReader();
    final ConfigAssembler assembler = new ConfigAssembler();
    final ConfigComposer composer;
    final LegacyColorizer colorizer = new LegacyColorizer(new HexColorNormalizer());
    final MessageBook messages;
    final PlayerDirectory players = new BukkitPlayerDirectory();
    final NoticeBoard notices;
    final OnlinePlayerCount online = new OnlinePlayerCount();
    final BedrockDetector bedrock;
    final StandingCache standings;
    final RegionLookup regionLookup;
    final RegionExemptions regions;
    final WorkerPool ioPool = new WorkerPool("io", 2);
    final WorkloadCounters workload = new WorkloadCounters();
    final ServerEnvironment environment;
    final ClientCensus clients;
    final BackendService backend;
    final MissingPresetNotices missingPreset;
    final PresetPoller presets;
    final ReleaseWatcher updates;
    final JoinNotices joinNotices;
    final BukkitPlayerEventBridge playerEvents;
    final StandingRefresher standingRefresher;
    final TickSource ticks;

    private CoreWiring(JavaPlugin plugin, PlatformProfile platform, MessageCatalog catalog) {
        this.plugin = plugin;
        this.platform = platform;
        this.version = buildVersion(plugin);
        this.config = new ConfigReference(EffectiveConfig.pluginDefaults(coreReader.read(ConfigTree.empty()).settings()));
        this.log = new ConsoleLog(new JulLogSink(plugin.getLogger()), redactor, () -> DebugTally.on(config.get().core().debug()), clock);
        this.address = LoopbackOverride.fromSystemProperty().resolve(log);
        this.bus = new SynchronousEventBus(log);
        this.scheduler = platform.folia() ? new FoliaTaskScheduler(plugin, new FoliaTickTiming())
                : new BukkitTaskScheduler(plugin, new BukkitTickTiming());
        this.resources = new ClasspathResources(plugin.getClass().getClassLoader());
        this.storageRoot = new DiskDataFolder(plugin.getDataFolder().toPath());
        this.localFile = new LocalConfigFile(storageRoot, resources, yaml, log);
        this.composer = new ConfigComposer(assembler, config, bus, log);
        this.messages = new MessageBook(catalog, config, colorizer);
        this.notices = new NoticeBoard(players, scheduler);
        this.bedrock = ReflectiveBedrockDetector.detect(plugin.getClass().getClassLoader());
        this.standings = new StandingCache(bedrock, new InstalledPermissionPlugin());
        this.regionLookup = WorldGuardRegionLookup.detect();
        this.regions = new RegionExemptions(() -> config.get().detection().regions(), regionLookup, log);
        ReplyObservers replies = new ReplyObservers(SharedNameAlarm.toConsoleAndStaff(() -> config.get().core().identity(), log,
                notices, colorizer, clock), new InterServerFeed(bus));
        this.environment = BukkitServerFacts.read(platform);
        this.clients = new ClientCensus(scheduler, players);
        this.backend = new BackendService(new JdkSessionSource(address, version,
                () -> new ServerAddress(Bukkit.getIp(), Bukkit.getPort()), scheduler, clock, random, log,
                online::count, replies, environment, clients), log);
        this.missingPreset = new MissingPresetNotices(messages, notices, log);
        this.presets = new PresetPoller(scheduler, new PresetFetcher(new JdkHttpTransport(
                new FreshClients(Duration.ofSeconds(5), ioPool.executor())), address.primary()), new PresetLayout(),
                assembler, composer, missingPreset, log, clock);
        this.updates = new ReleaseWatcher(scheduler, new JdkHttpTransport(new FreshClients(Duration.ofSeconds(8),
                ioPool.executor())), log, address.primary(), version, platform.updateChannel(),
                () -> config.get().modules().updates());
        this.joinNotices = new JoinNotices(notices, missingPreset, updates, colorizer);
        this.playerEvents = new BukkitPlayerEventBridge(plugin, bus, online, standings);
        this.standingRefresher = new StandingRefresher(scheduler, players, standings);
        this.ticks = chooseTickSource();
    }

    static CoreWiring create(JavaPlugin plugin, PlatformProfile platform) throws StartupFailure {
        try {
            MessageCatalog catalog = MessageCatalog.load(new ClasspathResources(plugin.getClass().getClassLoader()),
                    new SnakeYamlParser());
            return new CoreWiring(plugin, platform, catalog);
        } catch (YamlFormatException invalid) {
            throw new StartupFailure("Bundled message files are unreadable: " + invalid.getMessage(), invalid);
        }
    }

    private TickSource chooseTickSource() {
        if (platform.folia()) {
            return new PerPlayerTickSource(scheduler, bus, players);
        }
        return platform.tickEndEventAvailable() ? new PaperTickEndSource(plugin, bus)
                : new RepeatingTickSource(scheduler, bus);
    }

    @SuppressWarnings("deprecation")
    private static String buildVersion(JavaPlugin plugin) {
        return plugin.getDescription().getVersion();
    }
}
