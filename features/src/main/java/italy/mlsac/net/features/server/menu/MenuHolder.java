// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.menu;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {

    private final UUID viewer;
    private final Inventory inventory;

    @SuppressWarnings("deprecation")
    public MenuHolder(UUID viewer, int size, String title) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public UUID viewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
