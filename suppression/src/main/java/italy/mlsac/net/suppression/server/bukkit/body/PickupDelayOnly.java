// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.body;

import java.util.UUID;
import org.bukkit.entity.Item;

final class PickupDelayOnly implements ItemOwnerLock {

    @Override
    public void lockTo(Item item, UUID owner) {
    }
}
