// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.scoring.FlagContext;
import java.util.List;

public final class RungRunner {

    private final CommandRunner commands;
    private final PunishCooldown cooldown;
    private final PenaltyClock clock;
    private final Logbook log;

    public RungRunner(CommandRunner commands, PunishCooldown cooldown, PenaltyClock clock, Logbook log) {
        this.commands = commands;
        this.cooldown = cooldown;
        this.clock = clock;
        this.log = log;
    }

    public RungOutcome run(FlagContext target, List<String> configuredCommands) {
        List<LadderCommand> rung = parsed(configuredCommands);
        RungOutcome admission = admit(target, rung);
        if (admission == RungOutcome.RAN) {
            execute(target, rung, CommandOrigin.ladderStep(target.offenceLevel()));
        }
        return admission;
    }

    public void runWithoutCooldown(FlagContext target, List<String> configuredCommands, CommandOrigin origin) {
        execute(target, parsed(configuredCommands), origin);
    }

    private RungOutcome admit(FlagContext target, List<LadderCommand> rung) {
        if (rung.isEmpty()) {
            return RungOutcome.NOTHING_TO_RUN;
        }
        if (rung.stream().noneMatch(LadderCommand::punishing)) {
            return RungOutcome.RAN;
        }
        long now = clock.wallMillisNow();
        if (!cooldown.blocks(target.player(), now)) {
            cooldown.record(target.player(), now);
            return RungOutcome.RAN;
        }
        log.debug("[Penalty] Skipped rung for " + target.playerNick() + " (VL " + target.offenceLevel()
                + "): punishment cooldown");
        return RungOutcome.SKIPPED_COOLDOWN;
    }

    private void execute(FlagContext target, List<LadderCommand> rung, CommandOrigin origin) {
        for (LadderCommand command : rung) {
            commands.run(command, target, origin);
        }
    }

    private static List<LadderCommand> parsed(List<String> configuredCommands) {
        return configuredCommands.stream().map(LadderCommand::parse).toList();
    }
}
