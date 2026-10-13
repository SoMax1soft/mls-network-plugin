// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features;

import italy.mlsac.net.alerting.ActionBarText;
import italy.mlsac.net.alerting.AlertsSubcommand;
import italy.mlsac.net.alerting.MonitorSubcommand;
import italy.mlsac.net.alerting.ProbTracking;
import italy.mlsac.net.banimation.AnimationSubcommand;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.features.live.ActionBarRelay;
import italy.mlsac.net.features.live.BackgroundReplies;
import italy.mlsac.net.features.live.BundledMenuTexts;
import italy.mlsac.net.features.live.FeatureHooks;
import italy.mlsac.net.features.live.FeatureServer;
import italy.mlsac.net.features.live.NametagStage;
import italy.mlsac.net.features.live.ReloadRequests;
import italy.mlsac.net.features.live.SuspectRows;
import italy.mlsac.net.features.server.BukkitFeatureServer;
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
import italy.mlsac.net.profile.ProfileSubcommand;
import italy.mlsac.net.punishment.PunishSubcommand;
import italy.mlsac.net.restore.DataSubcommand;
import italy.mlsac.net.restore.RestorePace;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public final class FeaturesAssembly implements FeaturesModule {

    private static final String REINSTALL_CLOCK = "reinstall.confirm";
    private static final String RESTORE_CLOCK = "restore.pace";
    private static final String DOSSIER_CLOCK = "dossier.cache";

    private final FeatureWiring wiring;
    private final FeatureServer server;
    private final MenuDesk menus;
    private final NametagStage nametags;
    private final ProbTracking tracking;
    private final DossierDesk dossiers;
    private final DossierTimes dossierTimes;
    private final BackgroundReplies replies;
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
        this.dossiers = new DossierDesk(core.backend()::dossier, () -> core.clock().wallMillis(DOSSIER_CLOCK));
        this.dossierTimes = new DossierTimes(() -> core.clock().wallMillis(DOSSIER_CLOCK), ZoneId.systemDefault());
        this.replies = new BackgroundReplies(core.scheduler());
        this.menus = new MenuDesk(new BlueprintSource(BundledMenuTexts.load(core.resources(), core.yaml(), core.log()),
                () -> core.messages().serverLanguage()), () -> wiring.settings.get().menus(),
                new FramePainter(new LegacyColorizer(new HexColorNormalizer()), server.materials()),
                server.menuCanvas(rows), core.messages(), wiring.log, dossiers, () -> core.config().get().dossier(),
                dossierTimes);
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
                new AlertsSubcommand(wiring.alertSubscribers, core.messages()),
                new MonitorSubcommand(wiring.monitorSubscribers, tracking, wiring.lookup, core.messages()),
                new SuspectsSubcommand(() -> wiring.settings.get().menusOn(), menus, core.messages()),
                new ProfileSubcommand(wiring.lookup, wiring.facts, core.messages(), menus::openProfile,
                        () -> core.config().get().dossier(), dossiers, dossierTimes, replies),
                new ReloadSubcommand(reloads, core.messages(), wiring.log),
                new PunishSubcommand(wiring.lookup, wiring.facts, wiring.maximum, core.messages(), wiring.log),
                new ReinstallSubcommand(reloads, core.messages(), () -> core.clock().wallMillis(REINSTALL_CLOCK)),
                new AnimationSubcommand(wiring.library, wiring.animationFiles, core.messages(), wiring.log),
                new DataSubcommand(pipeline.restore(), wiring.lookup, () -> core.config().get().dataRestore().enabled(),
                        new RestorePace(() -> core.clock().wallMillis(RESTORE_CLOCK)), replies, core.messages()));
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
