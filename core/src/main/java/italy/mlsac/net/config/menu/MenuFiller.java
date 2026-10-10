// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import java.util.Optional;

public record MenuFiller(boolean enabled, Optional<String> material) {

    public static final String DEFAULT_MATERIAL = "BLACK_STAINED_GLASS_PANE";

    public String materialOrDefault() {
        return material.orElse(DEFAULT_MATERIAL);
    }
}
