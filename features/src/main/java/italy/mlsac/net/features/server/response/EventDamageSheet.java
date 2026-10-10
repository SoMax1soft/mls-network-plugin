// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.entity.EntityDamageEvent;

@SuppressWarnings("deprecation")
public final class EventDamageSheet implements DamageSheet {

    private final EntityDamageEvent event;

    public EventDamageSheet(EntityDamageEvent event) {
        this.event = event;
    }

    @Override
    public List<String> applicableParts() {
        List<String> parts = new ArrayList<>();
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (event.isApplicable(modifier)) {
                parts.add(modifier.name());
            }
        }
        return parts;
    }

    @Override
    public double part(String name) {
        return event.getDamage(EntityDamageEvent.DamageModifier.valueOf(name));
    }

    @Override
    public void rewritePart(String name, double value) {
        EntityDamageEvent.DamageModifier modifier = EntityDamageEvent.DamageModifier.valueOf(name);
        if (event.isApplicable(modifier)) {
            try {
                event.setDamage(modifier, value);
            } catch (UnsupportedOperationException fixedByServer) {
                return;
            }
        }
    }

    @Override
    public double baseDamage() {
        return event.getDamage();
    }

    @Override
    public void rewriteBase(double value) {
        event.setDamage(value);
    }

    @Override
    public double finalDamage() {
        return event.getFinalDamage();
    }
}
