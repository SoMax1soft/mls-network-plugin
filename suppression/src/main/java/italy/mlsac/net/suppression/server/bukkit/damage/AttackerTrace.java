// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.damage;

import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class AttackerTrace {

    private final CausingPlayer causing;

    public AttackerTrace(CausingPlayer causing) {
        this.causing = causing;
    }

    public UUID attackerOrNull(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            UUID direct = playerBehindOrNull(byEntity.getDamager());
            if (direct != null) {
                return direct;
            }
        }
        return causing.causingPlayerOrNull(event);
    }

    public static UUID playerBehindOrNull(Entity damager) {
        if (damager instanceof Player player) {
            return player.getUniqueId();
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter.getUniqueId();
        }
        return null;
    }
}
