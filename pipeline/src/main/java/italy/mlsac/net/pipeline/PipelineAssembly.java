// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline;

import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.PipelineModule;
import italy.mlsac.net.module.PipelinePorts;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.environment.CountedStates;
import italy.mlsac.net.pipeline.engine.InputTapSource;
import italy.mlsac.net.pipeline.live.AnimatingPlayers;
import italy.mlsac.net.pipeline.live.ConfiguredSwitches;
import italy.mlsac.net.pipeline.live.CoreTimeSources;
import italy.mlsac.net.pipeline.live.DiskFiles;
import italy.mlsac.net.pipeline.live.LegacyHistoryDesk;
import italy.mlsac.net.pipeline.live.LifeFeed;
import italy.mlsac.net.pipeline.live.PacketFeed;
import italy.mlsac.net.pipeline.live.PlayerFacts;
import italy.mlsac.net.pipeline.live.PlayerOrderedInput;
import italy.mlsac.net.pipeline.live.ServerSide;
import italy.mlsac.net.pipeline.live.WindowDispatch;
import italy.mlsac.net.pipeline.live.ServerHooks;
import italy.mlsac.net.pipeline.server.BukkitServerSide;
import italy.mlsac.net.pipeline.workload.MeteredInput;
import italy.mlsac.net.detection.AimProfiles;
import java.util.Objects;

public final class PipelineAssembly implements PipelineModule {

    private final PlayerOrderedInput pipeline;
    private final PacketFeed packetFeed;
    private final LifeFeed lifeFeed;
    private final ServerHooks serverHooks;
    private final PipelinePorts ports;

    public PipelineAssembly(CoreServices core) {
        this(core, InputTapSource.NONE);
    }

    public PipelineAssembly(CoreServices core, InputTapSource taps) {
        this(core, new BukkitServerSide(core).ports(), taps);
    }

    public PipelineAssembly(CoreServices core, ServerSide realServer) {
        this(core, realServer, InputTapSource.NONE);
    }

    public PipelineAssembly(CoreServices core, ServerSide realServer, InputTapSource taps) {
        Objects.requireNonNull(core, "core");
        CountedStates states = new CountedStates(realServer.states());
        ServerSide server = new ServerSide(states, realServer.rays(), realServer.facts(), realServer.hooks());
        AnimatingPlayers animations = new AnimatingPlayers();
        PlayerFacts facts = new PlayerFacts(core.standings(), core.regions(), animations, core.packets().clients(),
                core.players(), server.facts());
        CoreTimeSources time = new CoreTimeSources(core.clock(), core.random());
        DiskFiles storage = new DiskFiles(core.storageRoot());
        ConfiguredSwitches switches = new ConfiguredSwitches(core.config(), core.backend());
        WindowDispatch windows = new WindowDispatch(core.backend(), core.bus(), facts::name, core.log(),
                core.workload(), System::nanoTime);
        DataPipeline engine = new DataPipeline(new italy.mlsac.net.pipeline.engine.PipelinePorts(server.states(),
                facts, facts, server.rays(), time, time, time, storage, storage, switches, switches, windows,
                windows.legacy(), (viewer, id) -> core.packets().sender().sendTransactionPing(viewer, id)),
                Objects.requireNonNull(taps, "taps"));
        this.pipeline = new PlayerOrderedInput(engine);
        MeteredInput metered = new MeteredInput(pipeline, core.workload(), System::nanoTime, states::lastRosterSize);
        this.packetFeed = new PacketFeed(core.packets(), metered);
        this.lifeFeed = new LifeFeed(core.bus(), metered, animations);
        this.serverHooks = server.hooks();
        this.ports = new PipelinePorts(new LegacyHistoryDesk(pipeline), AimProfiles.NONE,
                pipeline::inLegacyCombat);
    }

    @Override
    public String name() {
        return "pipeline";
    }

    @Override
    public void start() {
        serverHooks.start();
        lifeFeed.start();
        packetFeed.start();
        pipeline.enabled();
    }

    @Override
    public void stop() {
        packetFeed.stop();
        lifeFeed.stop();
        serverHooks.stop();
        pipeline.disabled();
    }

    @Override
    public PipelinePorts ports() {
        return ports;
    }
}
