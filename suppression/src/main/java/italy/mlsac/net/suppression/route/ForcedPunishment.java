// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import java.util.UUID;

public final class ForcedPunishment {

    private final LegacyPenaltyLadder ladder;

    public ForcedPunishment(LegacyPenaltyLadder ladder) {
        this.ladder = ladder;
    }

    public void punish(UUID suspectId, String suspectName) {
        ladder.runTopRung(suspectId, suspectName);
    }
}
