// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import italy.mlsac.net.config.guard.GuardMode;

public interface GuardReport {

    void refused(ConsoleOrder order, GuardRefusal refusal, GuardMode mode);

    void burst(String rootKey, int players, int pauseMinutes, GuardMode mode);
}
