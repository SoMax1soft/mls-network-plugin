// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class TakenItems {

    static final int MAIN_SLOTS = 36;

    private final Map<UUID, Stash> stashes = new ConcurrentHashMap<>();

    public int strip(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> taken = new ArrayList<>();
        keep(taken, inventory.getHelmet());
        keep(taken, inventory.getChestplate());
        keep(taken, inventory.getLeggings());
        keep(taken, inventory.getBoots());
        keep(taken, inventory.getItemInOffHand());
        keep(taken, player.getItemOnCursor());
        for (int slot = 0; slot < MAIN_SLOTS; slot++) {
            keep(taken, inventory.getItem(slot));
        }
        inventory.setHelmet(null);
        inventory.setChestplate(null);
        inventory.setLeggings(null);
        inventory.setBoots(null);
        inventory.setItemInOffHand(null);
        inventory.clear();
        player.setItemOnCursor(null);
        stashes.put(player.getUniqueId(), new Stash(player.getWorld().getUID(), List.copyOf(taken)));
        return taken.size();
    }

    public Optional<ItemStack> take(UUID player, int index) {
        Stash stash = stashes.get(player);
        if (stash == null || index < 0 || index >= stash.items().size()) {
            return Optional.empty();
        }
        if (index == stash.items().size() - 1) {
            stashes.remove(player, stash);
        }
        return Optional.of(stash.items().get(index).clone());
    }

    public Optional<Stash> release(UUID player) {
        return Optional.ofNullable(stashes.remove(player));
    }

    private static void keep(List<ItemStack> taken, ItemStack item) {
        if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
            taken.add(item.clone());
        }
    }

    public record Stash(UUID world, List<ItemStack> items) {

        public List<ItemStack> from(int index) {
            return items.subList(Math.max(0, Math.min(index, items.size())), items.size());
        }
    }
}
