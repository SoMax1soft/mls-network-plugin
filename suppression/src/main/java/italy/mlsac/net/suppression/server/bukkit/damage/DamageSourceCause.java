// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.damage;

import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

final class DamageSourceCause implements CausingPlayer {

    @Override
    public UUID causingPlayerOrNull(EntityDamageEvent event) {
        Entity causing = event.getDamageSource().getCausingEntity();
        return causing instanceof Player player ? player.getUniqueId() : null;
    }
}
