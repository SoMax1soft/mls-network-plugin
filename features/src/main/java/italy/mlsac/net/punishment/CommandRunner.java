// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.guard.ConsoleGate;
import italy.mlsac.net.guard.ConsoleOrder;
import italy.mlsac.net.guard.PunishedPlayer;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.scoring.FlagContext;
import java.util.Optional;
import java.util.function.Supplier;

public final class CommandRunner {

    static final String UNSAFE_HISTORY_TEXT = "DIRECT_KICK (unsafe name)";
    static final String UNSAFE_REPORTED_COMMAND = "DIRECT_KICK";

    private final Supplier<PenaltyRules> rules;
    private final PenaltyServer server;
    private final PenaltyDispatch dispatch;
    private final KickHistory history;
    private final PenaltyClock clock;
    private final Logbook log;
    private final ConsoleGate gate;

    public CommandRunner(Supplier<PenaltyRules> rules, PenaltyServer server, PenaltyDispatch dispatch,
                         KickHistory history, PenaltyClock clock, Logbook log, ConsoleGate gate) {
        this.rules = rules;
        this.server = server;
        this.dispatch = dispatch;
        this.history = history;
        this.clock = clock;
        this.log = log;
        this.gate = gate;
    }

    public void run(LadderCommand command, FlagContext target, CommandOrigin origin) {
        try {
            execute(command, target, origin);
        } catch (RuntimeException failure) {
            log.severe("[Penalty] Command failed for " + target.playerNick() + ": " + command.configured(), failure);
        }
    }

    private void execute(LadderCommand command, FlagContext target, CommandOrigin origin) {
        if (rules.get().nickGuard() && command.consoleCommand() && !SafeNames.safe(target.playerNick())) {
            kickUnsafeName(command, target);
            return;
        }
        Optional<String> resolved = guarded(command, target, origin, PenaltyText.resolve(command.body(), target));
        if (resolved.isEmpty()) {
            return;
        }
        history.add(record(target, command.configured()));
        server.handedOver(command.configured());
        command.dispatch(dispatch, target.player(), resolved.get());
        if (command.punishing()) {
            server.punishReport(new PunishEvent(target.player(), target.playerNick(), target.probability(),
                    target.buffer(), target.offenceLevel(), command.reportedAction(), command.configured()));
        }
    }

    private Optional<String> guarded(LadderCommand command, FlagContext target, CommandOrigin origin,
                                     String resolved) {
        if (!command.consoleCommand()) {
            return Optional.of(resolved);
        }
        CommandOrigin source = command instanceof LadderCommand.Animated ? origin.animated() : origin;
        return gate.admit(ConsoleOrder.against(resolved, source,
                new PunishedPlayer(target.playerNick(), target.player())));
    }

    private void kickUnsafeName(LadderCommand command, FlagContext target) {
        log.warn("[Penalty] skipped console command for unusual nick " + target.playerNick()
                + ", kicked instead; configured command was: " + command.configured());
        history.add(record(target, UNSAFE_HISTORY_TEXT));
        server.kickNextTick(target.player(), "MLSAC Detection (VL:" + target.offenceLevel() + ")");
        server.punishReport(new PunishEvent(target.player(), target.playerNick(), target.probability(),
                target.buffer(), target.offenceLevel(), "kick", UNSAFE_REPORTED_COMMAND));
    }

    private KickLogLine record(FlagContext target, String commandText) {
        return new KickLogLine(target.playerNick(), target.probability(), target.buffer(), target.offenceLevel(),
                clock.localTimeStamp(), commandText);
    }
}
