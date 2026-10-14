// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features;

import italy.mlsac.net.alerting.AlertSoundResolver;
import italy.mlsac.net.alerting.AlertTexts;
import italy.mlsac.net.alerting.CrossServerFeed;
import italy.mlsac.net.alerting.StaffAlerts;
import italy.mlsac.net.alerting.StaffEnrollment;
import italy.mlsac.net.alerting.SubscriberBroadcast;
import italy.mlsac.net.alerting.SubscriberSet;
import italy.mlsac.net.banimation.AnimationDirector;
import italy.mlsac.net.banimation.AnimationLibrary;
import italy.mlsac.net.banimation.LibraryStage;
import italy.mlsac.net.banimation.ParticleNames;
import italy.mlsac.net.banimation.ScriptReader;
import italy.mlsac.net.banimation.StageExtras;
import italy.mlsac.net.features.live.AnimationTicker;
import italy.mlsac.net.features.live.BundledAnimations;
import italy.mlsac.net.features.live.BusHandoff;
import italy.mlsac.net.features.live.CrossServerRelay;
import italy.mlsac.net.features.live.DirectoryLookup;
import italy.mlsac.net.features.live.DirectoryStaff;
import italy.mlsac.net.features.live.FeatureServer;
import italy.mlsac.net.features.live.PenaltyDesk;
import italy.mlsac.net.features.live.PluginLogbook;
import italy.mlsac.net.features.live.RosterStates;
import italy.mlsac.net.features.live.SchedulerLanes;
import italy.mlsac.net.features.live.StaffLines;
import italy.mlsac.net.features.live.StagePenalties;
import italy.mlsac.net.features.live.SuspectAdmission;
import italy.mlsac.net.features.live.SuspectFacts;
import italy.mlsac.net.features.live.VerdictRouting;
import italy.mlsac.net.features.settings.LiveSettings;
import italy.mlsac.net.handoff.AuthorityRoute;
import italy.mlsac.net.integrity.BadPacketStrike;
import italy.mlsac.net.integrity.BadPacketTally;
import italy.mlsac.net.integrity.BadPacketWatch;
import italy.mlsac.net.handoff.PenaltyAuthority;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.logbook.WarnOnce;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.FeaturePorts;
import italy.mlsac.net.module.PipelinePorts;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.punishment.CommandRunner;
import italy.mlsac.net.punishment.KickHistory;
import italy.mlsac.net.punishment.LadderPenalties;
import italy.mlsac.net.punishment.MaximumPunishment;
import italy.mlsac.net.punishment.PenaltyRules;
import italy.mlsac.net.punishment.PunishCooldown;
import italy.mlsac.net.punishment.RungRunner;
import italy.mlsac.net.punishment.ServerPenaltyDispatch;
import italy.mlsac.net.response.DamageReductions;
import italy.mlsac.net.response.LegacyAlertResponses;
import italy.mlsac.net.response.TrickCooldowns;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.scoring.DetectionFlow;
import italy.mlsac.net.scoring.FlagProcessor;
import italy.mlsac.net.scoring.LegacyIntake;
import italy.mlsac.net.scoring.LegacySequenceReply;
import italy.mlsac.net.scoring.LegacyWindowLength;
import italy.mlsac.net.scoring.PlayerThread;
import italy.mlsac.net.scoring.SuspectRoster;
import italy.mlsac.net.scoring.UltraIntake;
import italy.mlsac.net.scoring.VerdictScreen;
import italy.mlsac.net.scoring.ViolationDecayRun;
import italy.mlsac.net.scoring.ViolationLedger;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.List;
import java.util.function.Supplier;

final class FeatureWiring {

    private static final String ANIMATION_CHANCE = "animation.random";
    private static final String RESPONSE_CLOCK = "responses.clock";
    private static final String BAD_PACKET_CLOCK = "badpackets.window";

    final LiveSettings settings;
    final Logbook log;
    final SchedulerLanes lanes;
    final SuspectRoster roster = new SuspectRoster(SuspectRoster.REJOIN_RETENTION);
    final ViolationLedger ledger = new ViolationLedger();
    final SubscriberSet alertSubscribers = new SubscriberSet();
    final SubscriberSet monitorSubscribers = new SubscriberSet();
    final DirectoryStaff staffRoster;
    final DirectoryLookup lookup;
    final SubscriberBroadcast broadcast;
    final StaffEnrollment enrollment;
    final CrossServerRelay crossServer;
    final PenaltyDesk desk;
    final AnimationLibrary library;
    final BundledAnimations animationFiles;
    final AnimationTicker animationTicker;
    final AnimationDirector director;
    final KickHistory removalLog = new KickHistory();
    final PunishCooldown punishCooldown = new PunishCooldown();
    final RungRunner rungs;
    final MaximumPunishment maximum;
    final DamageReductions reductions = new DamageReductions();
    final TrickCooldowns trickCooldowns = new TrickCooldowns();
    final LegacyAlertResponses legacyResponses;
    final VerdictRouting verdicts;
    final BadPacketWatch badPackets;
    final SuspectAdmission admission;
    final ViolationDecayRun decay;
    final SuspectFacts facts;
    final FeaturePorts ports;

    FeatureWiring(CoreServices core, PipelinePorts pipeline, FeatureServer server) {
        this.settings = new LiveSettings(core.config().get());
        this.log = new PluginLogbook(core.log());
        this.lanes = new SchedulerLanes(core.scheduler(), core.players());
        this.staffRoster = new DirectoryStaff(core.players(), server.staffSounds());
        this.lookup = new DirectoryLookup(core.players());
        AlertTexts texts = new AlertTexts(core.messages());
        this.broadcast = new SubscriberBroadcast(staffRoster, lanes, () -> settings.get().alerts(),
                new AlertSoundResolver(server.sounds(), new WarnOnce(log)), texts, log);
        StaffAlerts staffAlerts = new StaffAlerts(alertSubscribers, monitorSubscribers, texts, broadcast);
        this.enrollment = new StaffEnrollment(alertSubscribers, monitorSubscribers, staffRoster);
        this.crossServer = new CrossServerRelay(new CrossServerFeed(texts,
                model -> settings.get().models().displayName(model), staffRoster, lanes, log));
        this.desk = new PenaltyDesk(core.scheduler(), core.players(), core.notices(), core.backend(), core.clock(),
                server.console());
        this.library = new AnimationLibrary(new ScriptReader(new ParticleNames(server.effectRegistry(), log),
                server.effectRegistry(), StageExtras.IGNORED_AS_BEFORE, log), log);
        this.animationFiles = new BundledAnimations(core.resources(), core.yaml());
        this.animationTicker = new AnimationTicker(core.scheduler(), core.bus());
        this.director = new AnimationDirector(server.animationEffects(),
                () -> core.random().nextDouble(ANIMATION_CHANCE), log, animationTicker);
        Supplier<PenaltyRules> rules = () -> settings.get().penalties();
        CommandRunner runner = new CommandRunner(rules, desk, new ServerPenaltyDispatch(desk,
                new LibraryStage(library, director), desk, core.messages(), rules, log), removalLog, desk, log,
                server.consoleGate());
        this.rungs = new RungRunner(runner, punishCooldown, desk, log);
        this.maximum = new MaximumPunishment(rules, rungs);
        this.legacyResponses = new LegacyAlertResponses(() -> settings.get().responses(), lanes, reductions,
                trickCooldowns, server.tricks(), line -> core.notices().broadcast(Audience.STAFF_OR_OP, List.of(line)),
                new LegacyColorizer(new HexColorNormalizer()), () -> core.clock().wallMillis(RESPONSE_CLOCK), log);
        AuthorityRoute route = new AuthorityRoute(() -> PenaltyAuthority.of(settings.get().engineOwnsPenalties()),
                new LadderPenalties(rules, rungs), legacyResponses, new BusHandoff(core.bus(), roster, ledger));
        PlayerThread playerThreads = lanes;
        FlagProcessor flags = new FlagProcessor(ledger, playerThreads, staffAlerts, route);
        DetectionFlow flow = new DetectionFlow(() -> settings.get().scoring(), () -> settings.get().reporting(),
                new VerdictScreen(() -> settings.get().models(), log), staffAlerts, desk,
                route, playerThreads, flags, log);
        this.badPackets = new BadPacketWatch(new BadPacketTally(), () -> core.config().get().core().badPackets(),
                () -> settings.get().detectionEnabled(), () -> core.clock().monotonicMillis(BAD_PACKET_CLOCK),
                task -> core.scheduler().submit(TaskLane.BACKGROUND, TaskTiming.immediately(), task),
                new BadPacketStrike(roster, flow, staffAlerts), roster, log);
        this.verdicts = new VerdictRouting(roster, flow, new UltraIntake(),
                new LegacyIntake(new LegacySequenceReply(fixedLegacyWindow(core), log)));
        this.admission = new SuspectAdmission(roster, core.players(), core.standings(), core.regions(),
                () -> settings.get().detectionEnabled(), core.clock());
        this.decay = new ViolationDecayRun(ledger, () -> settings.get().vlDecay(),
                player -> !settings.get().models().ultraMode() && pipeline.combat().inLegacyCombat(player));
        this.facts = new SuspectFacts(roster, ledger, pipeline.aim(), core.packets().clients());
        this.ports = new FeaturePorts(new StagePenalties(rungs), new StaffLines(broadcast, alertSubscribers, desk),
                new RosterStates(roster, ledger));
    }

    private static LegacyWindowLength fixedLegacyWindow(CoreServices core) {
        return new LegacyWindowLength() {
            @Override
            public int current() {
                return core.config().get().detection().legacySequence();
            }

            @Override
            public void change(int newLength) {
            }
        };
    }
}
