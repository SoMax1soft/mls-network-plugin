// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.menu;

import italy.mlsac.net.menu.MenuDesk;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MenuClickRouting implements Listener {

    private final MenuDesk desk;
    private final OpenMenus menus;

    public MenuClickRouting(MenuDesk desk, OpenMenus menus) {
        this.desk = desk;
        this.menus = menus;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void clicked(InventoryClickEvent event) {
        if (ourMenu(event).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        UUID viewer = event.getWhoClicked().getUniqueId();
        if (pressedButton(event.getInventory(), event.getRawSlot()) && desk.owns(viewer)) {
            desk.clicked(viewer, event.getWhoClicked().getName(), event.getRawSlot(), ClickKinds.of(event.getClick()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void dragged(InventoryDragEvent event) {
        ourMenu(event).ifPresent(holder -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void closed(InventoryCloseEvent event) {
        ourMenu(event).ifPresent(menus::closed);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void left(PlayerQuitEvent event) {
        menus.forget(event.getPlayer().getUniqueId());
    }

    private static Optional<MenuHolder> ourMenu(InventoryEvent event) {
        return Optional.ofNullable(event.getInventory().getHolder())
                .filter(MenuHolder.class::isInstance)
                .map(MenuHolder.class::cast);
    }

    private static boolean pressedButton(Inventory top, int rawSlot) {
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return false;
        }
        ItemStack face = top.getItem(rawSlot);
        return face != null && !face.getType().isAir();
    }
}
