// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;

public record ItemFace(String material, String name, List<String> lore) {

    public static final String PLAYER_HEAD = "PLAYER_HEAD";
    public static final String FILLER_NAME = " ";

    public ItemFace {
        lore = List.copyOf(lore);
    }
}
