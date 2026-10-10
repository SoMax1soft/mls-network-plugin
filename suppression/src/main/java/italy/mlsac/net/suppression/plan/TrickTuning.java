// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public record TrickTuning(boolean bladeRequired, double sidewaysPush, double upwardPush,
                          String legacyMessage) {

    private static final double MAX_VELOCITY = 10.0;

    public TrickTuning {
        sidewaysPush = clamp(sidewaysPush);
        upwardPush = clamp(upwardPush);
        legacyMessage = legacyMessage == null ? "" : legacyMessage;
    }

    public static TrickTuning defaultsFor(TrickType type) {
        return new TrickTuning(true, type.profile().stockSideways(), type.profile().stockUpward(), "");
    }

    public static TrickTuning from(LegacyTrickEntry entry) {
        return new TrickTuning(entry.bladeRequired(), entry.sidewaysPush(), entry.upwardPush(),
                entry.message());
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(MAX_VELOCITY, value));
    }
}
