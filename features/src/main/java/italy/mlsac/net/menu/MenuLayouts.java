// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.Optional;

public record MenuLayouts(Optional<PresetMenu> suspects, Optional<PresetMenu> checks) {

    public static MenuLayouts shipped() {
        return new MenuLayouts(Optional.empty(), Optional.empty());
    }
}
