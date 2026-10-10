// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import java.util.UUID;

public final class LegacyMode implements ResponseMode {

    private final LegacyPenaltyLadder ladder;
    private final LegacyAlertResponses responses;

    public LegacyMode(LegacyPenaltyLadder ladder, LegacyAlertResponses responses) {
        this.ladder = ladder;
        this.responses = responses;
    }

    @Override
    public void onFlag(FlagEvent flag) {
        ladder.runFlag(flag);
    }

    @Override
    public void onAnswer(BufferAnswer answer) {
        responses.onAnswer(answer);
    }

    @Override
    public void onBufferLowered(UUID suspectId, double buffer) {
    }

    @Override
    public void leave() {
        responses.dropActiveReductions();
    }
}
