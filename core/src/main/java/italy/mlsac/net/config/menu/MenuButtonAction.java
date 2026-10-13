// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import java.util.Locale;
import java.util.Optional;

public enum MenuButtonAction {
    NONE("none"),
    CLOSE("close"),
    BACK("back"),
    COMMAND("command"),
    PREV_PAGE("prev-page"),
    NEXT_PAGE("next-page"),
    PAGE_INFO("page-info"),
    INFO("info"),
    KICKS("kicks");

    private final String id;

    MenuButtonAction(String id) {
        this.id = id;
    }

    public static Optional<MenuButtonAction> fromId(String raw) {
        String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (MenuButtonAction action : values()) {
            if (action.id.equals(id)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return id;
    }
}
