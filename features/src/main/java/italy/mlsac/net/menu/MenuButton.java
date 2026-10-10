// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;

public record MenuButton(int slot, ButtonAction action, String material, String name, List<String> lore,
                         String command) {

    public MenuButton {
        lore = List.copyOf(lore);
        command = command == null ? "" : command;
    }
}
