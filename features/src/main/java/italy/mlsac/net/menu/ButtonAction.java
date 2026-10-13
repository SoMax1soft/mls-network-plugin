// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.Locale;
import java.util.Optional;

public enum ButtonAction {
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

    ButtonAction(String id) {
        this.id = id;
    }

    public static Optional<ButtonAction> fromId(String raw) {
        String wanted = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (ButtonAction action : values()) {
            if (action.id.equals(wanted)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return id;
    }
}
