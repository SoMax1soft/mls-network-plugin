// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public record MenuBlueprint(String title, int size, List<Integer> listSlots, List<MenuButton> buttons,
                            Optional<String> fillerMaterial, String headTitle, List<String> headLoreLines) {

    public MenuBlueprint {
        TreeSet<Integer> slots = new TreeSet<>();
        for (int slot : listSlots) {
            if (slot >= 0 && slot < size) {
                slots.add(slot);
            }
        }
        listSlots = List.copyOf(slots);
        List<Integer> list = listSlots;
        buttons = buttons.stream().filter(button -> button.slot() >= 0 && button.slot() < size
                && !list.contains(button.slot())).toList();
        headLoreLines = List.copyOf(headLoreLines);
    }

    public int middleListSlot() {
        return listSlots.isEmpty() ? size / 2 : listSlots.get(listSlots.size() / 2);
    }

    public int slotsPerSheet() {
        return Math.max(1, listSlots.size());
    }

    public Optional<MenuButton> firstButtonAt(int slot) {
        return buttons.stream().filter(button -> button.slot() == slot).findFirst();
    }
}
