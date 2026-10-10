// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import italy.mlsac.net.config.CleanText;
import italy.mlsac.net.config.SettingsLayer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class MenuReader {

    private static final String ROOT = "menu-layouts";
    private static final Pattern MATERIAL = Pattern.compile("^[A-Z0-9_]{1,40}$");
    private static final int MAX_TEXT = 96;
    private static final int MAX_LORE = 10;
    private static final int MAX_BUTTONS = 54;
    private static final int MAX_COMMAND = 256;
    private static final String DEFAULT_BUTTON_MATERIAL = "PAPER";

    public MenuLayouts read(MenuLayouts lower, SettingsLayer layer) {
        Optional<SettingsLayer> section = layer.section(ROOT);
        if (section.isEmpty()) {
            return lower;
        }
        Map<MenuId, MenuSheetSpec> layouts = new EnumMap<>(MenuId.class);
        layouts.putAll(lower.layouts());
        for (MenuId id : MenuId.values()) {
            section.get().section(id.id()).ifPresent(menu -> layouts.put(id, readLayout(menu)));
        }
        return new MenuLayouts(layouts);
    }

    private MenuSheetSpec readLayout(SettingsLayer menu) {
        Optional<String> title = menu.text("title", MAX_TEXT);
        int rows = menu.intClamped("rows", 1, 6).orElse(6);
        MenuFiller filler = new MenuFiller(menu.bool("filler.enabled").orElse(true),
                menu.has("filler.material") ? material(menu, "filler.material") : Optional.empty());
        List<Integer> slots = readSlots(menu);
        MenuEntryTemplate entry = new MenuEntryTemplate(menu.text("entry.name", MAX_TEXT), readLore(menu, "entry.lore"));
        return new MenuSheetSpec(title, rows, filler, slots, entry, readButtons(menu));
    }

    private static Optional<String> material(SettingsLayer layer, String path) {
        return layer.matching(path, MATERIAL);
    }

    private static List<Integer> readSlots(SettingsLayer menu) {
        List<Integer> slots = new ArrayList<>();
        menu.list("list").ifPresent(list -> {
            for (Object raw : list) {
                if (raw instanceof Number number) {
                    slots.add((int) Math.max(0, Math.min(53, number.longValue())));
                }
            }
        });
        return slots;
    }

    private static Optional<List<String>> readLore(SettingsLayer layer, String path) {
        Optional<List<?>> list = layer.list(path);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>();
        for (Object raw : list.get()) {
            if (lines.size() >= MAX_LORE) {
                break;
            }
            if (raw instanceof String text) {
                lines.add(CleanText.of(text, MAX_TEXT));
            }
        }
        return Optional.of(lines);
    }

    private List<MenuButton> readButtons(SettingsLayer menu) {
        List<MenuButton> buttons = new ArrayList<>();
        Optional<List<?>> list = menu.list("buttons");
        if (list.isEmpty()) {
            return buttons;
        }
        for (int i = 0; i < list.get().size() && buttons.size() < MAX_BUTTONS; i++) {
            Object raw = list.get().get(i);
            String label = "buttons[" + i + "]";
            if (raw instanceof Map<?, ?> map) {
                readButton(menu.element(map, label)).ifPresent(buttons::add);
            } else {
                menu.drop(label, "not an object");
            }
        }
        return buttons;
    }

    private Optional<MenuButton> readButton(SettingsLayer button) {
        OptionalInt slot = button.intClamped("slot", 0, 53);
        Optional<MenuButtonAction> action = button.text("action", 16).flatMap(MenuButtonAction::fromId);
        Optional<String> material = button.has("material") ? material(button, "material")
                : Optional.of(DEFAULT_BUTTON_MATERIAL);
        if (slot.isEmpty() || action.isEmpty() || material.isEmpty()) {
            button.drop("", "button needs slot, a known action and a valid material");
            return Optional.empty();
        }
        Optional<String> command = action.get() == MenuButtonAction.COMMAND
                ? button.text("command", MAX_COMMAND)
                : Optional.empty();
        return Optional.of(new MenuButton(slot.getAsInt(), action.get(), material.get(),
                button.text("name", MAX_TEXT).orElse(" "), readLore(button, "lore").orElse(List.of()), command));
    }
}
