// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.module;

import italy.mlsac.net.config.SettingsLayer;

public final class ModuleReader {

    public ModuleSwitches read(ModuleSwitches lower, SettingsLayer layer) {
        return new ModuleSwitches(
                layer.bool("menus.enabled").orElse(lower.menus()),
                layer.bool("updates.enabled").orElse(lower.updates()));
    }
}
