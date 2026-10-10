// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.Locale;

public enum SuspectPose {
    STANDING(2.3),
    SNEAKING(1.95),
    LOW(1.0),
    SLEEPING(0.6);

    private final double tagHeight;

    SuspectPose(double tagHeight) {
        this.tagHeight = tagHeight;
    }

    public static SuspectPose fromGamePose(String gamePose) {
        String pose = gamePose == null ? "" : gamePose.trim().toUpperCase(Locale.ROOT);
        return switch (pose) {
            case "CROUCHING", "SNEAKING" -> SNEAKING;
            case "SWIMMING", "FALL_FLYING", "SPIN_ATTACK" -> LOW;
            case "SLEEPING" -> SLEEPING;
            default -> STANDING;
        };
    }

    public double tagHeight() {
        return tagHeight;
    }
}
