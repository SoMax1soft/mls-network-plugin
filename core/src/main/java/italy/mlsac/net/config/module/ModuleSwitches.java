// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.module;

public record ModuleSwitches(boolean menus, boolean updates) {

    public static ModuleSwitches pluginDefaults() {
        return new ModuleSwitches(true, true);
    }
}
