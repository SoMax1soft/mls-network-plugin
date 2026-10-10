// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record MenuLayouts(Map<MenuId, MenuSheetSpec> layouts) {

    public MenuLayouts {
        layouts = layouts.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(layouts));
    }

    public static MenuLayouts shipped() {
        return new MenuLayouts(Map.of());
    }

    public Optional<MenuSheetSpec> layout(MenuId id) {
        return Optional.ofNullable(layouts.get(id));
    }
}
