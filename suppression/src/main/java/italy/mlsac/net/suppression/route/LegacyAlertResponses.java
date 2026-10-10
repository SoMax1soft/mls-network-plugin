// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import italy.mlsac.net.suppression.trigger.BufferAnswer;

public interface LegacyAlertResponses {

    void onAnswer(BufferAnswer answer);

    void dropActiveReductions();
}
