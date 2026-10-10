// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.penalty;

import italy.mlsac.net.detection.PlayerFlagged;
import java.util.List;

public interface PenaltyCommands {

    PenaltyCommands NONE = new PenaltyCommands() {
        @Override
        public StageCommandOutcome runStage(PlayerFlagged flag, List<String> ladderCommands) {
            return StageCommandOutcome.NOTHING_TO_RUN;
        }

        @Override
        public boolean punishing(String ladderCommand) {
            return false;
        }
    };

    StageCommandOutcome runStage(PlayerFlagged flag, List<String> ladderCommands);

    boolean punishing(String ladderCommand);
}
