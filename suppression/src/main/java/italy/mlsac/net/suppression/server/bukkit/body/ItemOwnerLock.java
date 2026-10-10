// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.body;

import java.util.UUID;
import org.bukkit.entity.Item;

public interface ItemOwnerLock {

    void lockTo(Item item, UUID owner);

    static ItemOwnerLock forThisServer() {
        try {
            Item.class.getMethod("setOwner", UUID.class);
            return new OwnerLockByApi();
        } catch (NoSuchMethodException absent) {
            return new PickupDelayOnly();
        }
    }
}
