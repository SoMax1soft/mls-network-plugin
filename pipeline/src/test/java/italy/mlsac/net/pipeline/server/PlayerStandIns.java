// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

final class PlayerStandIns {

    private PlayerStandIns() {
    }

    static World world(UUID id, String label) {
        return StandIn.of(World.class).answer("getUID", id).answer("getName", label).build();
    }

    static StandIn<Player> standing(UUID id, int entityNumber, World world, double x, double y, double z) {
        PlayerInventory inventory = StandIn.of(PlayerInventory.class).answer("getItemInMainHand", null).build();
        return StandIn.of(Player.class)
                .answer("getUniqueId", id)
                .answer("getEntityId", entityNumber)
                .answer("getWorld", world)
                .compute("getLocation", arguments -> new Location(world, x, y, z, 12.5f, -3.25f))
                .answer("getEyeHeight", 1.6200000047683716)
                .answer("getHeight", 1.7999999523162842)
                .answer("getWidth", 0.6000000238418579)
                .answer("getFallDistance", 0.0f)
                .answer("getAttackCooldown", 0.7f)
                .answer("isValid", true)
                .answer("isDead", false)
                .answer("isOnline", true)
                .answer("isSprinting", true)
                .answer("isSneaking", false)
                .answer("isClimbing", false)
                .answer("isInWater", true)
                .answer("isInsideVehicle", false)
                .answer("getInventory", inventory)
                .answer("getGameMode", GameMode.SURVIVAL)
                .answer("getPing", 42);
    }
}
