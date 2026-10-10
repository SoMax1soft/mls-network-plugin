// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

public record PlayerPosture(
        boolean sprinting,
        boolean sneaking,
        boolean climbing,
        boolean submerged,
        boolean blind,
        boolean riding) {

    public static final PlayerPosture NEUTRAL = new PlayerPosture(false, false, false, false, false, false);
}
