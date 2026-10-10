// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

public record PenaltyRules(LadderRungs ladder, boolean banShowOn, String banShowStyle, boolean nickGuard,
                           boolean alertsConsole) {

    public static final String DEFAULT_ANIMATION = "classic_ban";

    public static PenaltyRules pluginDefaults() {
        return new PenaltyRules(LadderRungs.pluginDefaults(), true, DEFAULT_ANIMATION, false, false);
    }

    public String animationOrDefault() {
        return banShowStyle == null || banShowStyle.isBlank() ? DEFAULT_ANIMATION : banShowStyle;
    }
}
