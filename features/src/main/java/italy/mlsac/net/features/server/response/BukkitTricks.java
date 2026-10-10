// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import italy.mlsac.net.response.TrickHands;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class BukkitTricks implements TrickHands {

    static final String BLADE_SUFFIX = "_SWORD";

    private final ShuffleOrder order;

    public BukkitTricks(ShuffleOrder order) {
        this.order = order;
    }

    @Override
    public boolean scrambleInventory(UUID player) {
        return online(player).map(found -> {
            PlayerInventory inventory = found.getInventory();
            ItemStack[] contents = inventory.getContents();
            order.shuffle(contents);
            inventory.setContents(contents);
            found.updateInventory();
            return true;
        }).orElse(false);
    }

    @Override
    public boolean flingHeldItem(UUID player, boolean bladeRequired, double horizontal, double vertical) {
        return online(player).map(found -> {
            PlayerInventory inventory = found.getInventory();
            ItemStack held = inventory.getItemInMainHand();
            if (held == null || held.getType().isAir() || held.getAmount() <= 0) {
                return false;
            }
            if (bladeRequired && !held.getType().name().toUpperCase(Locale.ROOT).endsWith(BLADE_SUFFIX)) {
                return false;
            }
            ItemStack thrown = held.clone();
            inventory.setItemInMainHand(null);
            Location eye = found.getEyeLocation();
            Item dropped = found.getWorld().dropItem(eye, thrown);
            dropped.setVelocity(ThrowVelocity.along(eye.getDirection(), horizontal, vertical));
            return true;
        }).orElse(false);
    }

    @Override
    public boolean launch(UUID player, double horizontal, double vertical) {
        return online(player).map(found -> {
            found.setVelocity(ThrowVelocity.against(found.getLocation().getDirection(), horizontal, vertical));
            return true;
        }).orElse(false);
    }

    private static Optional<Player> online(UUID player) {
        return Optional.ofNullable(Bukkit.getPlayer(player));
    }
}
