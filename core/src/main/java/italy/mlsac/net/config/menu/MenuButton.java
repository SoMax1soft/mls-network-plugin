// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import java.util.List;
import java.util.Optional;

public record MenuButton(int slot, MenuButtonAction action, String material, String name, List<String> lore,
                         Optional<String> command) {

    public MenuButton {
        lore = List.copyOf(lore);
    }
}
