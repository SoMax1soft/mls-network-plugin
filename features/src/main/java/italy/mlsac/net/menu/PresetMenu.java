// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;
import java.util.Optional;

public record PresetMenu(Optional<String> title, int rows, boolean fillerEnabled, Optional<String> fillerMaterial,
                         List<Integer> list, Optional<String> headTitle, Optional<List<String>> headLoreLines,
                         List<MenuButton> buttons) {

    public PresetMenu {
        list = List.copyOf(list);
        headLoreLines = headLoreLines.map(List::copyOf);
        buttons = List.copyOf(buttons);
    }
}
