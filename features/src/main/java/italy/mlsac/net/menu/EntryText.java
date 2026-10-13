// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;

public record EntryText(String name, List<String> lore) {

    public EntryText {
        lore = List.copyOf(lore);
    }

    EntryText orLore(List<String> fallback) {
        return lore.isEmpty() ? new EntryText(name, fallback) : this;
    }
}
