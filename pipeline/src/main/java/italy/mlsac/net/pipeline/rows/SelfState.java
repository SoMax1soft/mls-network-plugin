// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.player.PlayerSample;

public record SelfState(
        boolean valid,
        double swingCharge,
        double fallBlocks,
        boolean sprinting,
        boolean sneaking,
        int weapon,
        String handMaterial,
        double swingsPerSecond,
        int modeOfPlay,
        double reach) {

    private static final SelfState ABSENT =
            new SelfState(false, -1.0, 0.0, false, false, 0, HeldItemFamily.UNSEEN_MATERIAL, -1.0, -1, -1.0);

    public static SelfState of(PlayerSample fresh) {
        if (fresh == null) {
            return ABSENT;
        }
        return new SelfState(true, fresh.swingCharge(), fresh.fallBlocks(), fresh.posture().sprinting(),
                fresh.posture().sneaking(), HeldItemFamily.familyCode(fresh.handMaterial()), fresh.handMaterial(), fresh.swingsPerSecond(),
                fresh.gameModeCode(), fresh.reach());
    }
}
