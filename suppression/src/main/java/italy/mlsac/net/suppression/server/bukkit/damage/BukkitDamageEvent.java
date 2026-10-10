// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.damage;

import italy.mlsac.net.suppression.damage.DamageEventModel;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

@SuppressWarnings("deprecation")
final class BukkitDamageEvent implements DamageEventModel {

    private final EntityDamageEvent event;
    private final ComponentFault fault;

    BukkitDamageEvent(EntityDamageEvent event, ComponentFault fault) {
        this.event = event;
        this.fault = fault;
    }

    @Override
    public double finalDamage() {
        return event.getFinalDamage();
    }

    @Override
    public boolean scaleComponents(double factor) {
        try {
            for (DamageModifier modifier : DamageModifier.values()) {
                if (event.isApplicable(modifier)) {
                    event.setDamage(modifier, event.getDamage(modifier) * factor);
                }
            }
            return true;
        } catch (RuntimeException | LinkageError unavailable) {
            fault.report(unavailable);
            return false;
        }
    }

    @Override
    public double baseDamage() {
        return event.getDamage();
    }

    @Override
    public void baseDamage(double value) {
        event.setDamage(value);
    }

    @Override
    public void cancel() {
        event.setCancelled(true);
    }
}
