// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.chain;

import italy.mlsac.net.suppression.trigger.TriggerContext;

public record DueStage(TriggerContext context, ChainStage stage) {
}
