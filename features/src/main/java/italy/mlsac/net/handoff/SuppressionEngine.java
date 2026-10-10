// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.handoff;

import italy.mlsac.net.scoring.AnswerMoment;
import italy.mlsac.net.scoring.FlagContext;
import java.util.UUID;

public interface SuppressionEngine {

    void flagged(FlagContext flag);

    void bufferRaised(AnswerMoment moment);

    void bufferLowered(UUID player, double buffer);
}
