// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import italy.mlsac.net.suppression.chain.ChainBoard;
import italy.mlsac.net.suppression.execution.BufferTriggerRunner;
import italy.mlsac.net.suppression.execution.CurrentPlan;
import italy.mlsac.net.suppression.execution.StageRunner;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.suspect.SuspectRegistry;
import italy.mlsac.net.suppression.time.MonotonicClock;

public record EngineParts(CurrentPlan plan, SuspectRegistry registry, ChainBoard chains, StageRunner stages,
                          BufferTriggerRunner bufferTriggers, SuspectExits exits, EffectWatcher watcher,
                          SuppressionNotices notices, MonotonicClock clock) {
}
