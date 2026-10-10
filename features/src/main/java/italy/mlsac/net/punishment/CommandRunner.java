// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.scoring.FlagContext;
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

    public CommandRunner(Supplier<PenaltyRules> rules, PenaltyServer server, PenaltyDispatch dispatch,
                         KickHistory history, PenaltyClock clock, Logbook log) {
        this.rules = rules;
        this.server = server;
        this.dispatch = dispatch;
        this.history = history;
        this.clock = clock;
        this.log = log;
    }

    public void run(LadderCommand command, FlagContext target) {
        try {
            execute(command, target);
        } catch (RuntimeException failure) {
            log.severe("[Penalty] Command failed for " + target.playerNick() + ": " + command.configured(), failure);
        }
    }

    private void execute(LadderCommand command, FlagContext target) {
        if (rules.get().nickGuard() && command.consoleCommand() && !SafeNames.safe(target.playerNick())) {
            kickUnsafeName(command, target);
            return;
        }
        history.add(record(target, command.configured()));
        server.handedOver(command.configured());
        command.dispatch(dispatch, target.player(), PenaltyText.resolve(command.body(), target));
        if (command.punishing()) {
            server.punishReport(new PunishEvent(target.player(), target.playerNick(), target.probability(),
                    target.buffer(), target.offenceLevel(), command.reportedAction(), command.configured()));
        }
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
