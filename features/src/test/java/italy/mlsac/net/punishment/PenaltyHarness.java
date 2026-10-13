// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.guard.ConsoleGate;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PenaltyHarness implements PenaltyServer, AnimationStage, StaffNotices, PenaltyClock {

    final RecordingLog log = new RecordingLog();
    final List<String> console = new ArrayList<>();
    final List<String> kicks = new ArrayList<>();
    final List<PunishEvent> punishEvents = new ArrayList<>();
    final List<String> handedOver = new ArrayList<>();
    final List<String> staffLines = new ArrayList<>();
    final List<String> animations = new ArrayList<>();
    final Set<String> knownAnimations = new HashSet<>(Set.of("classic_ban"));
    final Set<UUID> online = new HashSet<>();
    final KickHistory history = new KickHistory();
    final PunishCooldown cooldown = new PunishCooldown();
    PenaltyRules rules = PenaltyRules.pluginDefaults();
    long now = 1_789_000_000_000L;
    final RungRunner rungs;
    final LadderPenalties ladder;
    final MaximumPunishment maximum;
    final UUID player = UUID.nameUUIDFromBytes("Steve".getBytes());

    PenaltyHarness() {
        this(ConsoleGate.OPEN);
    }

    PenaltyHarness(ConsoleGate gate) {
        online.add(player);
        ServerPenaltyDispatch dispatch = new ServerPenaltyDispatch(this, this, this, BundledResources.english(),
                () -> rules, log);
        CommandRunner commands = new CommandRunner(() -> rules, this, dispatch, history, this, log, gate);
        rungs = new RungRunner(commands, cooldown, this, log);
        ladder = new LadderPenalties(() -> rules, rungs);
        maximum = new MaximumPunishment(() -> rules, rungs);
    }

    void ladder(Map<Integer, List<String>> rungs) {
        rules = new PenaltyRules(LadderRungs.of(rungs), rules.banShowOn(), rules.banShowStyle(),
                rules.nickGuard(), rules.alertsConsole());
    }

    FlagContext flag(String name, int offenceLevel) {
        return new FlagContext(player, name, offenceLevel, 0.97, 20.0);
    }

    @Override
    public void dispatchConsoleNextTick(String command) {
        console.add(command);
    }

    @Override
    public boolean online(UUID id) {
        return online.contains(id);
    }

    @Override
    public void kickNextTick(UUID id, String reason) {
        kicks.add(reason);
    }

    @Override
    public void punishReport(PunishEvent event) {
        punishEvents.add(event);
    }

    @Override
    public void handedOver(String configuredCommand) {
        handedOver.add(configuredCommand);
    }

    @Override
    public boolean knows(String animation) {
        return knownAnimations.contains(animation);
    }

    @Override
    public void play(UUID id, String animation, String command) {
        animations.add(animation + "|" + command);
    }

    @Override
    public void toAllStaff(String coloredLine) {
        staffLines.add(coloredLine);
    }

    @Override
    public long wallMillisNow() {
        return now;
    }

    @Override
    public String localTimeStamp() {
        return "12:34:56";
    }
}
