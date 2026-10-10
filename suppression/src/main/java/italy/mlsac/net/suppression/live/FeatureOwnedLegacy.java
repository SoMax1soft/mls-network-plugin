// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.suppression.route.LegacyAlertResponses;
import italy.mlsac.net.suppression.route.LegacyPenaltyLadder;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import java.util.UUID;

public final class FeatureOwnedLegacy implements LegacyPenaltyLadder, LegacyAlertResponses {

    @Override
    public void runFlag(FlagEvent flag) {
    }

    @Override
    public void runTopRung(UUID suspectId, String suspectName) {
    }

    @Override
    public void onAnswer(BufferAnswer answer) {
    }

    @Override
    public void dropActiveReductions() {
    }
}
