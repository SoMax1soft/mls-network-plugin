// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;

public record AttackerState(int weapon, String handMaterial, double swingsPerSecond, int modeOfPlay, double reach,
                            double swingCharge, double fallBlocks, boolean grounded, PlayerPosture posture,
                            boolean critical) {

    private static final double CRITICAL_COOLDOWN = 0.9;

    public static AttackerState of(PlayerSample own, boolean grounded) {
        return own == null ? unseen(grounded) : seen(own, grounded);
    }

    private static AttackerState unseen(boolean grounded) {
        return new AttackerState(0, HeldItemFamily.UNSEEN_MATERIAL, -1.0, -1, -1.0, -1.0, 0.0, grounded,
                PlayerPosture.NEUTRAL, false);
    }

    private static AttackerState seen(PlayerSample own, boolean grounded) {
        String material = own.handMaterial();
        return new AttackerState(HeldItemFamily.familyCode(material), material, own.swingsPerSecond(),
                own.gameModeCode(), own.reach(), own.swingCharge(), own.fallBlocks(), grounded, own.posture(),
                landsCritical(own, grounded));
    }

    private static boolean landsCritical(PlayerSample own, boolean grounded) {
        PlayerPosture body = own.posture();
        boolean falling = !grounded && own.fallBlocks() > 0;
        boolean hindered = body.climbing() || body.submerged() || body.blind() || body.riding() || body.sprinting();
        return falling && !hindered && own.swingCharge() > CRITICAL_COOLDOWN;
    }
}
