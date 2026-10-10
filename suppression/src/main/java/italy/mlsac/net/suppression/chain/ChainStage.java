// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.chain;

import italy.mlsac.net.suppression.plan.StagePlan;

public record ChainStage(int position, int total, long startAt, StagePlan plan) {
}
