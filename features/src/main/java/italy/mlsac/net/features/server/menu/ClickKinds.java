// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.menu;

import italy.mlsac.net.menu.ClickKind;
import org.bukkit.event.inventory.ClickType;

public final class ClickKinds {

    private ClickKinds() {
    }

    public static ClickKind of(ClickType click) {
        if (click == null) {
            return ClickKind.OTHER;
        }
        return switch (click) {
            case LEFT -> ClickKind.LEFT;
            case SHIFT_LEFT -> ClickKind.SHIFT_LEFT;
            case RIGHT -> ClickKind.RIGHT;
            case SHIFT_RIGHT -> ClickKind.SHIFT_RIGHT;
            default -> ClickKind.OTHER;
        };
    }
}
