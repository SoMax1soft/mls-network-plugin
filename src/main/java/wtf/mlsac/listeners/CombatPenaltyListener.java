package wtf.mlsac.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import wtf.mlsac.Main;
import wtf.mlsac.response.DetectionResponseManager;

public class CombatPenaltyListener implements Listener {
    private final Main plugin;
    private final DetectionResponseManager detectionResponseManager;

    public CombatPenaltyListener(Main plugin, DetectionResponseManager detectionResponseManager) {
        this.plugin = plugin;
        this.detectionResponseManager = detectionResponseManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        double multiplier = detectionResponseManager.getDamageMultiplier(attacker.getUniqueId());
        if (multiplier >= 0.9999D) {
            return;
        }

        double original = event.getDamage();
        event.setDamage(original * multiplier);
        plugin.debug("[Responses] Damage reduction applied to " + attacker.getName()
                + ": " + String.format(java.util.Locale.ROOT, "%.2f -> %.2f (x%.2f)",
                        original, original * multiplier, multiplier));
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }
}
