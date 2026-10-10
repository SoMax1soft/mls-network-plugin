// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.player.PlayerSample;

record TargetChoice(PlayerSample target, int source, int inRange) {
    static final int FROM_NOTHING = 0;
    static final int FROM_LAST_ATTACK = 1;
    static final int FROM_VIEW = 2;
    static final TargetChoice NONE = new TargetChoice(null, FROM_NOTHING, 0);
}
