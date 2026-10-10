// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.menu;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class OpenMenus {

    private final Map<UUID, MenuHolder> shown = new ConcurrentHashMap<>();

    public void opened(MenuHolder holder) {
        shown.put(holder.viewer(), holder);
    }

    public Optional<MenuHolder> viewedBy(Player viewer) {
        MenuHolder holder = shown.get(viewer.getUniqueId());
        if (holder == null || !holder.getInventory().getViewers().contains(viewer)) {
            return Optional.empty();
        }
        return Optional.of(holder);
    }

    public void closed(MenuHolder holder) {
        shown.remove(holder.viewer(), holder);
    }

    public void forget(UUID viewer) {
        shown.remove(viewer);
    }
}
