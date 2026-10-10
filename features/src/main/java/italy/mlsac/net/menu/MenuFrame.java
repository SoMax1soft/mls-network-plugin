// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record MenuFrame(String title, int size, Map<Integer, ItemFace> items) {

    public MenuFrame {
        items = Collections.unmodifiableMap(new TreeMap<>(items));
    }

    public Optional<ItemFace> at(int slot) {
        return Optional.ofNullable(items.get(slot));
    }
}
