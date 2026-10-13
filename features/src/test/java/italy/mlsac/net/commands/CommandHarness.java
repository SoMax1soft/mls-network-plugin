// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.commands;

import italy.mlsac.net.alerting.ActionBarPorts;
import italy.mlsac.net.command.CommandRouter;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.clock.SystemClock;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.alerting.ActionBarText;
import italy.mlsac.net.alerting.AlertsSubcommand;
import italy.mlsac.net.alerting.MonitorSubcommand;
import italy.mlsac.net.alerting.ProbTracking;
import italy.mlsac.net.alerting.SubscriberSet;
import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.detection.RestoreAnswer;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.backend.session.LenientJson;
import italy.mlsac.net.detection.RestoreOutcome;
import italy.mlsac.net.detection.RestoreRequest;
import italy.mlsac.net.restore.DataSubcommand;
import italy.mlsac.net.restore.RestorePace;
import italy.mlsac.net.guard.ConsoleGate;
import italy.mlsac.net.maintenance.ReinstallSubcommand;
import italy.mlsac.net.maintenance.ReloadDesk;
import italy.mlsac.net.maintenance.ReloadSubcommand;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.profile.ProfileFacts;
import italy.mlsac.net.profile.ProfileSubcommand;
import italy.mlsac.net.punishment.CommandRunner;
import italy.mlsac.net.punishment.KickHistory;
import italy.mlsac.net.punishment.LadderRungs;
import italy.mlsac.net.punishment.MaximumPunishment;
import italy.mlsac.net.punishment.PenaltyClock;
import italy.mlsac.net.punishment.PenaltyDispatch;
import italy.mlsac.net.punishment.PenaltyRules;
import italy.mlsac.net.punishment.PenaltyServer;
import italy.mlsac.net.punishment.PunishCooldown;
import italy.mlsac.net.punishment.PunishEvent;
import italy.mlsac.net.punishment.PunishSubcommand;
import italy.mlsac.net.punishment.RungRunner;
import italy.mlsac.net.scoring.SuspectKind;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectSnapshot;
import italy.mlsac.net.scoring.ViolationLedger;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

final class CommandHarness implements PlayerLookup, ProfileFacts, ReloadDesk, ActionBarPorts,
        PenaltyServer, PenaltyClock, CommandLanes {

    final MessageBook messages = BundledResources.english();
    final RecordingLog log = new RecordingLog();
    final Map<String, UUID> online = new HashMap<>();
    final Map<UUID, SuspectRecord> records = new HashMap<>();
    final ViolationLedger ledger = new ViolationLedger();
    final KickHistory history = new KickHistory();
    final SubscriberSet alerts = new SubscriberSet();
    final SubscriberSet monitors = new SubscriberSet();
    final List<String> console = new ArrayList<>();
    final List<String> actionBars = new ArrayList<>();
    final List<BooleanSupplier> repeating = new ArrayList<>();
    PenaltyRules rules = new PenaltyRules(LadderRungs.of(Map.of(1, List.of("kick {PLAYER}"), 3,
            List.of("ban {PLAYER} {VL}"))), true, "classic_ban", false, false);
    long now = 1_000;
    boolean syncSucceeds = true;
    boolean restoreEnabled = true;
    final List<RestoreRequest> restores = new ArrayList<>();
    final List<Runnable> background = new ArrayList<>();
    final List<String> openedProfiles = new ArrayList<>();
    final List<String> dossierQueries = new ArrayList<>();
    DossierSettings dossierSettings = DossierSettings.pluginDefaults();
    String profileAnswer = "{\"player\":\"Steve\",\"known\":true,\"firstSeen\":0,\"lastSeen\":0,"
            + "\"detections\":{\"total\":42,\"day\":3,\"week\":12,\"max\":0.973,\"avg\":0.884,"
            + "\"models\":[{\"model\":\"pro\",\"count\":30,\"max\":0.973}]},"
            + "\"punishments\":{\"total\":3,\"kicks\":2,\"bans\":1,\"last\":{\"t\":0,\"action\":\"animation\"}},"
            + "\"suppressions\":{\"total\":5,\"latest\":[]},\"latest\":[]}";
    int reloads;
    final CommandRouter command;

    CommandHarness() {
        ProbTracking tracking = new ProbTracking(this, new ActionBarText(messages), messages);
        PenaltyDispatch dispatch = new PenaltyDispatch() {
            @Override
            public void console(String resolvedCommand) {
                console.add(resolvedCommand);
            }

            @Override
            public void staffNotice(String resolvedText) {
            }

            @Override
            public void animated(UUID player, String resolvedCommand, Optional<String> animationId) {
                console.add(resolvedCommand);
            }
        };
        RungRunner rungs = new RungRunner(new CommandRunner(() -> rules, this, dispatch, history, this, log,
                ConsoleGate.OPEN),
                new PunishCooldown(), this, log);
        PunishSubcommand.PunishEvidence evidence = new PunishSubcommand.PunishEvidence() {
            @Override
            public int offenceLevel(UUID player) {
                return ledger.level(player);
            }

            @Override
            public double buffer(UUID player) {
                return Optional.ofNullable(records.get(player)).map(SuspectRecord::buffer).orElse(0.0);
            }

            @Override
            public int highProbabilityCount(UUID player) {
                return Optional.ofNullable(records.get(player)).map(SuspectRecord::highProbabilityCount).orElse(0);
            }
        };
        command = new CommandRouter(messages, TestLogs.capturing(new CapturingLogSink(), new SystemClock(), false));
        command.registerAll(List.of(
                new AlertsSubcommand(alerts, messages),
                new MonitorSubcommand(monitors, tracking, this, messages),
                new ProfileSubcommand(this, this, messages, (viewer, viewerName, player) -> openedProfiles.add(
                        viewerName + " " + player), () -> dossierSettings, new DossierDesk(this::dossier, () -> now),
                        new DossierTimes(() -> 3_600_000L, ZoneOffset.UTC), this),
                new ReloadSubcommand(this, messages, log),
                new PunishSubcommand(this, evidence, new MaximumPunishment(() -> rules, rungs), messages, log),
                new DataSubcommand(this::restore, this, () -> restoreEnabled, new RestorePace(() -> now), this,
                        messages),
                new ReinstallSubcommand(this, messages, () -> now),
                new Subcommand() {
                    @Override
                    public SubcommandName name() {
                        return SubcommandName.SUSPECTS;
                    }

                    @Override
                    public void execute(CommandActor invoker, List<String> args) {
                        console.add("suspects menu for " + invoker.name());
                    }
                }));
    }

    UUID join(String name) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes());
        online.put(name, id);
        records.put(id, new SuspectRecord(id, name, SuspectKind.JAVA));
        return id;
    }

    List<String> run(FakeInvoker invoker, String line) {
        invoker.received.clear();
        command.dispatch(invoker, line.isEmpty() ? List.of() : List.of(line.split(" ", -1)));
        return invoker.received;
    }

    @Override
    public Optional<Target> byNamePrefix(String name) {
        return online.entrySet().stream().filter(entry -> entry.getKey().toLowerCase().startsWith(name.toLowerCase()))
                .map(entry -> new Target(entry.getValue(), entry.getKey())).findFirst();
    }

    @Override
    public Optional<Target> byExactName(String name) {
        return online.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(entry -> new Target(entry.getValue(), entry.getKey())).findFirst();
    }

    @Override
    public List<String> onlineNames() {
        return online.keySet().stream().sorted().toList();
    }

    @Override
    public OptionalInt sensitivityPercent(UUID player) {
        return OptionalInt.empty();
    }

    @Override
    public Optional<String> clientVersion(UUID player) {
        return Optional.of("V_1_21_4");
    }

    @Override
    public int highProbabilityCount(UUID player) {
        return records.get(player).highProbabilityCount();
    }

    @Override
    public void scheduleFullReload() {
        reloads++;
    }

    @Override
    public int reloadAnimations() {
        return 3;
    }

    @Override
    public boolean syncConfigAndReload() {
        return syncSucceeds;
    }

    private RestoreAnswer restore(RestoreRequest request) {
        restores.add(request);
        return records.get(request.player()).hasChecks()
                ? new RestoreAnswer(RestoreOutcome.SAVED, 120, 7, "data/restore/x")
                : RestoreAnswer.of(RestoreOutcome.NOTHING_BUFFERED);
    }

    @Override
    public void inBackground(Runnable task) {
        background.add(task);
    }

    @Override
    public void reply(CommandActor actor, List<String> lines) {
        lines.forEach(actor::send);
    }

    private CompletableFuture<DossierReply> dossier(DossierQuery query) {
        dossierQueries.add(query.path());
        return CompletableFuture.completedFuture(new DossierReply.Found(LenientJson.object(profileAnswer)
                .orElseThrow()));
    }

    void runBackground() {
        List<Runnable> pending = new ArrayList<>(background);
        background.clear();
        pending.forEach(Runnable::run);
    }

    @Override
    public boolean online(UUID player) {
        return online.containsValue(player);
    }

    @Override
    public void showHotbarText(UUID viewer, String coloredText) {
        actionBars.add(coloredText);
    }

    @Override
    public Optional<SuspectSnapshot> snapshot(UUID target) {
        return Optional.ofNullable(records.get(target)).filter(SuspectRecord::hasChecks).map(SuspectRecord::snapshot);
    }

    @Override
    public int offenceLevel(UUID target) {
        return ledger.level(target);
    }

    @Override
    public void repeatOnViewerThread(UUID viewer, long everyTicks, BooleanSupplier runWhileTrue) {
        if (runWhileTrue.getAsBoolean()) {
            repeating.add(runWhileTrue);
        }
    }

    @Override
    public void dispatchConsoleNextTick(String command) {
        console.add(command);
    }

    @Override
    public void kickNextTick(UUID player, String reason) {
    }

    @Override
    public void punishReport(PunishEvent event) {
    }

    @Override
    public long wallMillisNow() {
        return now;
    }

    @Override
    public String localTimeStamp() {
        return "10:00:00";
    }

}
