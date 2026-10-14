// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.backend.status.StatusSubcommand;
import italy.mlsac.net.command.CommandRouter;
import italy.mlsac.net.command.bukkit.BukkitCommandBridge;
import italy.mlsac.net.config.ConfigChangeCause;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.local.LocalConfigException;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.features.FeaturesAssembly;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.HostHandles;
import italy.mlsac.net.module.PluginModule;
import italy.mlsac.net.packet.packetevents.PacketEventsLayer;
import italy.mlsac.net.packet.packetevents.PacketEventsRuntime;
import italy.mlsac.net.packet.packetevents.PacketLibraryException;
import italy.mlsac.net.pipeline.PipelineAssembly;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.platform.ServerVersionRules;
import italy.mlsac.net.player.BedrockDetector;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.suppression.SuppressionAssembly;
import italy.mlsac.net.telemetry.bukkit.BukkitClientBrands;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

final class PluginRuntime {

    private static final String COMMAND = "mlsac";

    private final JavaPlugin plugin;
    private final PlatformProfile platform;
    private CoreWiring core;
    private PacketEventsRuntime packetRuntime;
    private PacketEventsLayer packets;
    private ModuleSet modules;
    private DebugPulse debugPulse;
    private final ShutdownSteps shutdown = new ShutdownSteps();

    PluginRuntime(JavaPlugin plugin, PlatformProfile platform) {
        this.plugin = plugin;
        this.platform = platform;
    }

    void enable() throws StartupFailure {
        long started = System.nanoTime();
        core = CoreWiring.create(plugin, platform);
        platform.serverVersion().warnings().forEach(line -> core.log.warn(LogTopic.PLATFORM, line));
        startPacketLibrary();
        EffectiveConfig config = loadConfiguration();
        CoreServices services = services();
        Edition edition = new PluginEdition(services);
        PipelineAssembly pipeline = new PipelineAssembly(services, edition.inputTaps());
        FeaturesAssembly features = new FeaturesAssembly(services, pipeline.ports());
        SuppressionAssembly suppression = new SuppressionAssembly(services, features.ports(), edition.lagExemptions());
        List<PluginModule> all = new ArrayList<>(List.of(pipeline, features, suppression));
        all.addAll(edition.extraModules());
        modules = new ModuleSet(all, core.log);
        modules.registerMessages(core.messages);
        modules.startAll();
        if (config.detection().enabled()) {
            core.backend.start(config.core());
        }
        core.online.seed(core.players.online().stream().map(PlayerView::id).toList());
        core.ticks.start();
        core.standingRefresher.start();
        core.clients.start();
        core.playerEvents.start();
        core.bus.subscribe(PlayerLeft.class, left -> core.regions.forget(left.player()));
        debugPulse = new DebugPulse(core, platform, packets);
        registerCommand(services, edition);
        core.joinNotices.start(core.bus);
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        new StartupSummary().lines(core.version, millis, platform, config, core.address.primary(), integrations(),
                        edition)
                .forEach(line -> core.log.info(LogTopic.GENERAL, line));
        core.updates.start();
        core.presets.start(config.core(), config.detection().enabled());
        debugPulse.start();
    }

    void disable() {
        if (core == null) {
            return;
        }
        CoreWiring wiring = core;
        shutdown.run(wiring.log, "debug pulse", () -> {
            if (debugPulse != null) {
                debugPulse.stop();
            }
        });
        shutdown.run(wiring.log, "tick source", wiring.ticks::stop);
        shutdown.run(wiring.log, "standing refresh", wiring.standingRefresher::stop);
        shutdown.run(wiring.log, "client census", wiring.clients::stop);
        shutdown.run(wiring.log, "modules", () -> {
            if (modules != null) {
                modules.haltEvery();
            }
        });
        shutdown.run(wiring.log, "preset poller", wiring.presets::stop);
        shutdown.run(wiring.log, "update checker", wiring.updates::stop);
        shutdown.run(wiring.log, "join notices", wiring.joinNotices::stop);
        shutdown.run(wiring.log, "backend", () -> stopBackend(wiring));
        shutdown.run(wiring.log, "player events", wiring.playerEvents::stop);
        shutdown.run(wiring.log, "packet listeners", () -> {
            if (packets != null) {
                packets.close();
            }
        });
        shutdown.run(wiring.log, "packet library", () -> {
            if (packetRuntime != null) {
                packetRuntime.stop();
            }
        });
        shutdown.run(wiring.log, "io pool", wiring.ioPool::close);
        shutdown.run(wiring.log, "scheduler", wiring.scheduler::revokeEvery);
        wiring.redactor.clear();
        core = null;
    }

    private void startPacketLibrary() throws StartupFailure {
        try {
            packetRuntime = PacketEventsRuntime.start(plugin);
            packets = new PacketEventsLayer(packetRuntime, platform.serverVersion().version(), core.log);
            core.clients.attach(packets.clients(), BukkitClientBrands.detect());
        } catch (PacketLibraryException failure) {
            throw new StartupFailure("Packet library could not start: " + failure.getMessage(), failure);
        }
    }

    private EffectiveConfig loadConfiguration() throws StartupFailure {
        try {
            ConfigurationLoad load = new ConfigurationLoad(core);
            return load.readLocal(ConfigChangeCause.STARTUP);
        } catch (LocalConfigException failure) {
            throw new StartupFailure("Cannot load config.yml: " + failure.getMessage(), failure);
        }
    }

    private CoreServices services() {
        return new CoreServices(core.version, platform, new ServerVersionRules(packets.serverVersion()),
                core.scheduler, core.clock, core.random, core.log, core.bus, core.config, core.messages, core.backend,
                packets, core.players, core.notices, core.standings, core.regions, core.online, core.storageRoot,
                core.resources, core.yaml, new ReloadProcedure(core), HostHandles.of(plugin),
                core.workload);
    }

    private void registerCommand(CoreServices services, Edition edition) {
        CommandRouter router = new CommandRouter(core.messages, core.log);
        router.register(new StatusSubcommand(core.backend, core.config, core.messages, core.scheduler));
        router.register(new DebugSubcommand(debugPulse, core.players, core.messages));
        router.registerAll(modules.subcommands().stream().filter(verb -> edition.offers(verb.name())).toList());
        PluginCommand command = plugin.getCommand(COMMAND);
        if (command == null) {
            core.log.error(LogTopic.COMMANDS, "Command /" + COMMAND + " is missing from plugin.yml");
            return;
        }
        BukkitCommandBridge bridge = new BukkitCommandBridge(router);
        command.setExecutor(bridge);
        command.setTabCompleter(bridge);
    }

    private StartupSummary.Integrations integrations() {
        return new StartupSummary.Integrations(core.regionLookup.available(), core.bedrock != BedrockDetector.NONE);
    }

    private static void stopBackend(CoreWiring wiring) {
        wiring.log.info(LogTopic.GENERAL, "Closing the backend connection");
        try {
            wiring.backend.stop().get(5, TimeUnit.SECONDS);
        } catch (Exception failure) {
            wiring.log.warn(LogTopic.BACKEND, "Backend connection did not close cleanly: " + failure);
        }
    }
}
