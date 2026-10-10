// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import italy.mlsac.net.suppression.trigger.FlagEvent;
import java.util.UUID;

public interface LegacyPenaltyLadder {

    void runFlag(FlagEvent flag);

    void runTopRung(UUID suspectId, String suspectName);
}
