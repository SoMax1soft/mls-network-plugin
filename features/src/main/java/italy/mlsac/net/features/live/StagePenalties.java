// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.detection.PlayerFlagged;
import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.penalty.PenaltyCommands;
import italy.mlsac.net.penalty.StageCommandOutcome;
import italy.mlsac.net.punishment.LadderCommand;
import italy.mlsac.net.punishment.RungRunner;
import italy.mlsac.net.scoring.FlagContext;
import java.util.List;

public final class StagePenalties implements PenaltyCommands {

    private final RungRunner rungs;

    public StagePenalties(RungRunner rungs) {
        this.rungs = rungs;
    }

    @Override
    public StageCommandOutcome runStage(PlayerFlagged flag, List<String> ladderCommands) {
        if (ladderCommands.isEmpty()) {
            return StageCommandOutcome.NOTHING_TO_RUN;
        }
        rungs.runWithoutCooldown(new FlagContext(flag.player(), flag.playerNick(), flag.offenceLevel(),
                flag.probability(), flag.buffer()), ladderCommands,
                CommandOrigin.suppressionStage(flag.offenceLevel()));
        return StageCommandOutcome.RAN;
    }

    @Override
    public boolean punishing(String ladderCommand) {
        return LadderCommand.parse(ladderCommand).punishing();
    }
}
