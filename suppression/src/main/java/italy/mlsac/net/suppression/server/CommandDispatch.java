// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import italy.mlsac.net.suppression.trigger.TriggerContext;

public interface CommandDispatch {

    boolean punishing(String ladderText);

    void run(String ladderText, TriggerContext context);
}
