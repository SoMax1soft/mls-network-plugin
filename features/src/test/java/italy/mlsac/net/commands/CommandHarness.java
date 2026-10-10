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
import italy.mlsac.net.detection.RestoreResult;
import italy.mlsac.net.history.FalsepositiveSubcommand;
import italy.mlsac.net.maintenance.ReinstallSubcommand;
import italy.mlsac.net.maintenance.ReloadDesk;
import italy.mlsac.net.maintenance.ReloadSubcommand;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.profile.ProbSubcommand;
import italy.mlsac.net.profile.ProfileFacts;
import italy.mlsac.net.profile.ProfileSubcommand;
import italy.mlsac.net.punishment.CommandRunner;
import italy.mlsac.net.punishment.KickHistory;
import italy.mlsac.net.punishment.KicklistSubcommand;
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

import java.util.UUID;
import java.util.function.BooleanSupplier;

final class CommandHarness implements PlayerLookup, ProfileFacts, ReloadDesk, ActionBarPorts,
        PenaltyServer, PenaltyClock {

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
            public void animated(UUID player, String resolvedCommand) {
                console.add(resolvedCommand);
            }
        };
        RungRunner rungs = new RungRunner(new CommandRunner(() -> rules, this, dispatch, history, this, log),
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
                new ProbSubcommand(messages),
                new ProfileSubcommand(this, this, messages),
                new ReloadSubcommand(this, messages, log),
                new PunishSubcommand(this, evidence, new MaximumPunishment(() -> rules, rungs), messages, log),
                new KicklistSubcommand(history, messages),
                new FalsepositiveSubcommand(this::restore, this, messages),
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

    private RestoreResult restore(UUID player) {
        return records.get(player).hasChecks() ? RestoreResult.SAVED : RestoreResult.NOTHING_STORED;
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
