// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.penalty;

public record PenaltySettings(boolean banShowOn, String banShowStyle, PenaltyLadder ladder,
                              boolean nickGuard) {

    public static final String DEFAULT_ANIMATION = "classic_ban";

    public static PenaltySettings pluginDefaults() {
        return new PenaltySettings(true, DEFAULT_ANIMATION, PenaltyLadder.pluginDefaults(), false);
    }
}
