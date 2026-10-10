// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.damage;

import java.util.UUID;
import org.bukkit.event.entity.EntityDamageEvent;

@FunctionalInterface
public interface CausingPlayer {

    UUID causingPlayerOrNull(EntityDamageEvent event);

    static CausingPlayer forThisServer() {
        try {
            EntityDamageEvent.class.getMethod("getDamageSource");
            return new DamageSourceCause();
        } catch (NoSuchMethodException absent) {
            return event -> null;
        }
    }
}
