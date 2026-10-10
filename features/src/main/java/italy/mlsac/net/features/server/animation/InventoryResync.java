// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.features.server.OwningThread;
import italy.mlsac.net.schedule.TaskTiming;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public final class InventoryResync {

    private static final TaskTiming ONE_TICK_ON = TaskTiming.after(1L);

    private final OwningThread threads;

    public InventoryResync(OwningThread threads) {
        this.threads = threads;
    }

    public void refresh(Player suspect) {
        suspect.updateInventory();
        threads.laterWithPlayer(suspect.getUniqueId(), ONE_TICK_ON, Player::updateInventory);
    }

    public void shutAgain(Player suspect) {
        threads.laterWithPlayer(suspect.getUniqueId(), ONE_TICK_ON, HumanEntity::closeInventory);
    }
}
