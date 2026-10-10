// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.handoff;

import italy.mlsac.net.scoring.AnswerMoment;
import italy.mlsac.net.scoring.BufferReactions;
import italy.mlsac.net.scoring.SuspectRecord;

public final class SuppressionBufferReactions implements BufferReactions {

    private final SuppressionEngine engine;

    public SuppressionBufferReactions(SuppressionEngine engine) {
        this.engine = engine;
    }

    @Override
    public void bufferMoved(AnswerMoment moment) {
        engine.bufferRaised(moment);
    }

    @Override
    public void answerAccepted(AnswerMoment moment) {
    }

    @Override
    public void bufferReset(SuspectRecord suspect, double buffer) {
        engine.bufferLowered(suspect.id(), buffer);
    }
}
