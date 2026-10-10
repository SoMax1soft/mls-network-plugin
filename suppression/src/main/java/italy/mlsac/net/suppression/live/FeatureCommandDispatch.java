// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.detection.PlayerFlagged;
import italy.mlsac.net.penalty.PenaltyCommands;
import italy.mlsac.net.suppression.server.CommandDispatch;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.List;

public final class FeatureCommandDispatch implements CommandDispatch {

    private final PenaltyCommands penalties;

    public FeatureCommandDispatch(PenaltyCommands penalties) {
        this.penalties = penalties;
    }

    @Override
    public boolean punishing(String ladderText) {
        return penalties.punishing(ladderText);
    }

    @Override
    public void run(String ladderText, TriggerContext context) {
        penalties.runStage(new PlayerFlagged(context.suspectId(), context.suspectName(), context.vl(),
                context.probability(), context.buffer(), false), List.of(ladderText));
    }
}
