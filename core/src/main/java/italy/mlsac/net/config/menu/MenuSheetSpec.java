// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public record MenuSheetSpec(Optional<String> title, int rows, MenuFiller filler, List<Integer> listSlots,
                         MenuEntryTemplate entry, List<MenuButton> buttons) {

    public MenuSheetSpec {
        listSlots = List.copyOf(listSlots);
        buttons = List.copyOf(buttons);
    }

    public List<Integer> renderedListSlots() {
        TreeSet<Integer> slots = new TreeSet<>();
        for (int slot : listSlots) {
            if (slot < rows * 9) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    public List<MenuButton> renderedButtons() {
        List<Integer> list = renderedListSlots();
        return buttons.stream()
                .filter(button -> button.slot() < rows * 9 && !list.contains(button.slot()))
                .toList();
    }
}
