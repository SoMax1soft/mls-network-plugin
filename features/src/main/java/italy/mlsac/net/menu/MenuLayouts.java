// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.Optional;

public record MenuLayouts(Optional<PresetMenu> suspects, Optional<PresetMenu> checks, Optional<PresetMenu> kicks,
                          Optional<PresetMenu> profile) {

    public static MenuLayouts shipped() {
        return new MenuLayouts(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public MenuLayouts(Optional<PresetMenu> suspects, Optional<PresetMenu> checks) {
        this(suspects, checks, Optional.empty(), Optional.empty());
    }
}
