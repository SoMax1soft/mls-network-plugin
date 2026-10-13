// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

public enum OriginKind {
    LADDER("ladder", "ladder step VL {VL}"),
    LADDER_ANIMATION("ladder-animation", "ladder step VL {VL}, ban animation"),
    PUNISH("punish", "/mlsac punish"),
    PUNISH_ANIMATION("punish-animation", "/mlsac punish, ban animation"),
    SUPPRESSION("suppression", "suppression stage VL {VL}"),
    SUPPRESSION_ANIMATION("suppression-animation", "suppression stage VL {VL}, ban animation"),
    MENU_BUTTON("menu-button", "menu button");

    private final String id;
    private final String english;

    OriginKind(String id, String english) {
        this.id = id;
        this.english = english;
    }

    public String id() {
        return id;
    }

    public String english() {
        return english;
    }

    public OriginKind animated() {
        return switch (this) {
            case LADDER -> LADDER_ANIMATION;
            case PUNISH -> PUNISH_ANIMATION;
            case SUPPRESSION -> SUPPRESSION_ANIMATION;
            default -> this;
        };
    }
}
