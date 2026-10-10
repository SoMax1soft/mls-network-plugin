// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.body;

import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.server.SuspectBody;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

final class BukkitSuspectBody implements SuspectBody {

    private final Player player;
    private final ItemOwnerLock ownerLock;

    BukkitSuspectBody(Player player, ItemOwnerLock ownerLock) {
        this.player = player;
        this.ownerLock = ownerLock;
    }

    @Override
    public UUID id() {
        return player.getUniqueId();
    }

    @Override
    public boolean dead() {
        return player.isDead();
    }

    @Override
    public boolean creativeOrSpectator() {
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    @Override
    public boolean gliding() {
        return player.isGliding();
    }

    @Override
    public boolean mounted() {
        return player.isInsideVehicle();
    }

    @Override
    public Vec3 lookDirection() {
        Vector direction = player.getLocation().getDirection();
        return new Vec3(direction.getX(), direction.getY(), direction.getZ());
    }

    @Override
    public int storageSize() {
        return player.getInventory().getStorageContents().length;
    }

    @Override
    public void permuteStorage(int[] sourceSlotForSlot) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] before = inventory.getStorageContents();
        ItemStack[] after = new ItemStack[before.length];
        for (int slot = 0; slot < after.length; slot++) {
            after[slot] = before[sourceSlotForSlot[slot]];
        }
        inventory.setStorageContents(after);
        player.updateInventory();
    }

    @Override
    public Optional<String> mainHandMaterial() {
        Material material = player.getInventory().getItemInMainHand().getType();
        return material.isAir() ? Optional.empty() : Optional.of(material.name());
    }

    @Override
    public void dropMainHand(Vec3 velocity, int pickupDelayTicks) {
        PlayerInventory inventory = player.getInventory();
        ItemStack weapon = inventory.getItemInMainHand();
        if (weapon.getType().isAir()) {
            return;
        }
        inventory.setItemInMainHand(new ItemStack(Material.AIR));
        Item dropped = player.getWorld().dropItem(player.getEyeLocation(), weapon);
        dropped.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z()));
        dropped.setPickupDelay(pickupDelayTicks);
        ownerLock.lockTo(dropped, player.getUniqueId());
        player.updateInventory();
    }

    @Override
    public void pushVelocity(Vec3 velocity) {
        player.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z()));
    }
}
