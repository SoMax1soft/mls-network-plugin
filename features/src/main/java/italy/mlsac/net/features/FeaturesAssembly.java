// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features;

import italy.mlsac.net.alerting.ActionBarText;
import italy.mlsac.net.alerting.AlertsSubcommand;
import italy.mlsac.net.alerting.MonitorSubcommand;
import italy.mlsac.net.alerting.ProbTracking;
import italy.mlsac.net.banimation.AnimationSubcommand;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.features.live.ActionBarRelay;
import italy.mlsac.net.features.live.BundledMenuTexts;
import italy.mlsac.net.features.live.FeatureHooks;
import italy.mlsac.net.features.live.FeatureServer;
import italy.mlsac.net.features.live.NametagStage;
import italy.mlsac.net.features.live.ReloadRequests;
import italy.mlsac.net.features.live.SuspectRows;
import italy.mlsac.net.features.server.BukkitFeatureServer;
import italy.mlsac.net.history.FalsepositiveSubcommand;
import italy.mlsac.net.maintenance.ReinstallSubcommand;
import italy.mlsac.net.maintenance.ReloadSubcommand;
import italy.mlsac.net.menu.BlueprintSource;
import italy.mlsac.net.menu.FramePainter;
import italy.mlsac.net.menu.MenuDesk;
import italy.mlsac.net.menu.SuspectsSubcommand;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.FeaturePorts;
import italy.mlsac.net.module.FeaturesModule;
import italy.mlsac.net.module.PipelinePorts;
import italy.mlsac.net.profile.ProbSubcommand;
import italy.mlsac.net.profile.ProfileSubcommand;
import italy.mlsac.net.punishment.KicklistSubcommand;
import italy.mlsac.net.punishment.PunishSubcommand;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.List;
import java.util.Objects;

public final class FeaturesAssembly implements FeaturesModule {

    private static final String REINSTALL_CLOCK = "reinstall.confirm";

    private final FeatureWiring wiring;
    private final FeatureServer server;
    private final MenuDesk menus;
    private final NametagStage nametags;
    private final ProbTracking tracking;
    private final FeatureRhythm rhythm;
    private final List<Subcommand> subcommands;

    public FeaturesAssembly(CoreServices core, PipelinePorts pipeline) {
        this(core, pipeline, new BukkitFeatureServer(core));
    }

    public FeaturesAssembly(CoreServices core, PipelinePorts pipeline, FeatureServer server) {
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(pipeline, "pipeline");
        this.server = Objects.requireNonNull(server, "server");
        this.wiring = new FeatureWiring(core, pipeline, server);
        SuspectRows rows = new SuspectRows(wiring.roster, core.players());
        this.menus = new MenuDesk(new BlueprintSource(BundledMenuTexts.load(core.resources(), core.yaml(), core.log()),
                () -> core.messages().serverLanguage()), () -> wiring.settings.get().menus(),
                new FramePainter(new LegacyColorizer(new HexColorNormalizer()), server.materials()),
                server.menuCanvas(rows), core.messages(), wiring.log);
        this.nametags = new NametagStage(() -> wiring.settings.get().nametags(), core.packets().serverVersion(),
                server.nametagPorts(rows), core.scheduler(), core.log(), core.platform().folia(), core.workload());
        this.tracking = new ProbTracking(new ActionBarRelay(core.players(), core.scheduler(), wiring.roster,
                wiring.ledger), new ActionBarText(core.messages()), core.messages());
        this.rhythm = new FeatureRhythm(core, wiring, menus, nametags, tracking);
        this.subcommands = subcommands(core, pipeline);
    }

    private List<Subcommand> subcommands(CoreServices core, PipelinePorts pipeline) {
        ReloadRequests reloads = new ReloadRequests(core.reloader(), wiring.library, wiring.animationFiles);
        return List.of(
                new FalsepositiveSubcommand(pipeline.history(), wiring.lookup, core.messages()),
                new AlertsSubcommand(wiring.alertSubscribers, core.messages()),
                new MonitorSubcommand(wiring.monitorSubscribers, tracking, wiring.lookup, core.messages()),
                new SuspectsSubcommand(() -> wiring.settings.get().menusOn(), menus, core.messages()),
                new ProbSubcommand(core.messages()),
                new ProfileSubcommand(wiring.lookup, wiring.facts, core.messages()),
                new ReloadSubcommand(reloads, core.messages(), wiring.log),
                new PunishSubcommand(wiring.lookup, wiring.facts, wiring.maximum, core.messages(), wiring.log),
                new KicklistSubcommand(wiring.removalLog, core.messages()),
                new ReinstallSubcommand(reloads, core.messages(), () -> core.clock().wallMillis(REINSTALL_CLOCK)),
                new AnimationSubcommand(wiring.library, wiring.director, wiring.animationFiles, wiring.lookup,
                        core.messages(), wiring.log));
    }

    @Override
    public String name() {
        return "features";
    }

    @Override
    public void start() {
        rhythm.start();
        nametags.start();
        server.start(new FeatureHooks(menus, wiring.director, wiring.reductions, nametags, wiring.settings));
    }

    @Override
    public void stop() {
        server.stop();
        nametags.stop();
        rhythm.stop();
    }

    @Override
    public List<Subcommand> subcommands() {
        return subcommands;
    }

    @Override
    public FeaturePorts ports() {
        return wiring.ports;
    }
}
