// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.damage;

import italy.mlsac.net.suppression.damage.DamageSuppression;
import italy.mlsac.net.suppression.lag.runtime.CombatLedger;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.server.bukkit.cadence.ObservedTicks;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DamageWatch implements Listener {

    private final DamageSuppression damage;
    private final AttackerTrace attackers;
    private final ComponentFault fault;
    private final LagRuntimes<?> runtimes;
    private final CombatLedger combat;
    private final ObservedTicks ticks;
    private final MonotonicClock clock;

    public DamageWatch(DamageSuppression damage, AttackerTrace attackers, ComponentFault fault,
                       LagRuntimes<?> runtimes, CombatLedger combat, ObservedTicks ticks, MonotonicClock clock) {
        this.damage = damage;
        this.attackers = attackers;
        this.fault = fault;
        this.runtimes = runtimes;
        this.combat = combat;
        this.ticks = ticks;
        this.clock = clock;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void judge(EntityDamageEvent event) {
        UUID attacker = attackers.attackerOrNull(event);
        UUID victim = event.getEntity() instanceof Player player ? player.getUniqueId() : null;
        if (!damage.concerns(attacker, victim)) {
            return;
        }
        long tick = ticks.observedTickOf(attacker != null ? attacker : victim);
        damage.judgeDamage(new BukkitDamageEvent(event, fault), event.getCause().name(), attacker, victim, tick);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void landed(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID victim = player.getUniqueId();
        runtimes.knockedBack(victim, ticks.observedTickOf(victim));
        UUID attacker = AttackerTrace.playerBehindOrNull(event.getDamager());
        if (attacker != null && !attacker.equals(victim)) {
            combat.hurtByPlayer(victim, clock.clockMillis());
        }
    }
}
