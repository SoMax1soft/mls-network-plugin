// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import java.util.UUID;

public interface TrickHands {

    boolean scrambleInventory(UUID player);

    boolean flingHeldItem(UUID player, boolean bladeRequired, double horizontal, double vertical);

    boolean launch(UUID player, double horizontal, double vertical);
}
