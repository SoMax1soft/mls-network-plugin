// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Objects;

public record CommandOrigin(OriginKind kind, int level) {

    public static final String LEVEL = "{VL}";

    public CommandOrigin {
        Objects.requireNonNull(kind, "kind");
    }

    public static CommandOrigin ladderStep(int level) {
        return new CommandOrigin(OriginKind.LADDER, level);
    }

    public static CommandOrigin punish(int level) {
        return new CommandOrigin(OriginKind.PUNISH, level);
    }

    public static CommandOrigin suppressionStage(int level) {
        return new CommandOrigin(OriginKind.SUPPRESSION, level);
    }

    public static CommandOrigin menuButton() {
        return new CommandOrigin(OriginKind.MENU_BUTTON, 0);
    }

    public CommandOrigin animated() {
        return new CommandOrigin(kind.animated(), level);
    }

    public String english() {
        return kind.english().replace(LEVEL, String.valueOf(level));
    }
}
