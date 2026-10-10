// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import italy.mlsac.net.response.DamageReductions;
import java.util.Optional;
import java.util.function.LongSupplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ReducedDamageWatch implements Listener {

    private final DamageReductions reductions;
    private final LongSupplier clock;

    public ReducedDamageWatch(DamageReductions reductions, LongSupplier clock) {
        this.reductions = reductions;
        this.clock = clock;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void struck(EntityDamageByEntityEvent event) {
        attacker(event.getDamager()).ifPresent(player -> {
            double multiplier = reductions.multiplier(player.getUniqueId(), clock.getAsLong());
            if (multiplier < 1.0) {
                DamageDampening.scale(new EventDamageSheet(event), multiplier);
            }
        });
    }

    private static Optional<Player> attacker(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player);
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return Optional.of(shooter);
        }
        return Optional.empty();
    }
}
