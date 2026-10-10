// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.List;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

final class InteractionRangeAttribute implements ReachReading {

    private static final List<String> KEYS_IN_LOOKUP_ORDER =
            List.of("entity_interaction_range", "player.entity_interaction_range");

    private final PluginLog log;
    private volatile Optional<Attribute> resolved;

    InteractionRangeAttribute(PluginLog log) {
        this.log = log;
    }

    @Override
    public Double reach(Player player) {
        Optional<Attribute> attribute = attribute();
        if (attribute.isEmpty()) {
            return null;
        }
        AttributeInstance instance = player.getAttribute(attribute.get());
        return instance == null ? null : instance.getValue();
    }

    private Optional<Attribute> attribute() {
        Optional<Attribute> known = resolved;
        if (known == null) {
            known = lookUp();
            resolved = known;
        }
        return known;
    }

    private Optional<Attribute> lookUp() {
        try {
            for (String key : KEYS_IN_LOOKUP_ORDER) {
                Attribute found = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
                if (found != null) {
                    return Optional.of(found);
                }
            }
            return Optional.empty();
        } catch (RuntimeException | LinkageError unsupported) {
            log.debug(LogTopic.DATA, () -> "No interaction-range attribute on this server: " + unsupported);
            return Optional.empty();
        }
    }
}
