// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.settings;

import italy.mlsac.net.config.menu.MenuId;
import italy.mlsac.net.config.menu.MenuSheetSpec;
import italy.mlsac.net.menu.ButtonAction;
import italy.mlsac.net.menu.MenuButton;
import italy.mlsac.net.menu.MenuLayouts;
import italy.mlsac.net.menu.PresetMenu;
import java.util.Optional;

final class MenuTranslation {

    MenuLayouts translate(italy.mlsac.net.config.menu.MenuLayouts layouts) {
        return new MenuLayouts(sheet(layouts, MenuId.SUSPECTS), sheet(layouts, MenuId.CHECKS),
                sheet(layouts, MenuId.KICKS), sheet(layouts, MenuId.PROFILE));
    }

    private static Optional<PresetMenu> sheet(italy.mlsac.net.config.menu.MenuLayouts layouts, MenuId id) {
        return Optional.ofNullable(layouts.layouts().get(id)).map(MenuTranslation::menu);
    }

    private static PresetMenu menu(MenuSheetSpec spec) {
        return new PresetMenu(spec.title(), spec.rows(), spec.filler().enabled(), spec.filler().material(),
                spec.listSlots(), spec.entry().name(), spec.entry().lore(),
                spec.buttons().stream().map(MenuTranslation::button).flatMap(Optional::stream).toList());
    }

    private static Optional<MenuButton> button(italy.mlsac.net.config.menu.MenuButton button) {
        return ButtonAction.fromId(button.action().id()).map(action -> new MenuButton(button.slot(), action,
                button.material(), button.name(), button.lore(), button.command().orElse("")));
    }
}
