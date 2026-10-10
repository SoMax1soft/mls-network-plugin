// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.menu;

import java.util.List;
import java.util.Optional;

public record MenuEntryTemplate(Optional<String> name, Optional<List<String>> lore) {

    public MenuEntryTemplate {
        lore = lore.map(List::copyOf);
    }
}
