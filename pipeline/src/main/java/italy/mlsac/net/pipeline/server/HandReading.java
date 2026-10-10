// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class HandReading {

    static final String EMPTY_HAND = "air";
    static final String UNREADABLE_HAND = "unknown";

    private final PluginLog log;
    private final Map<Material, String> names = new ConcurrentHashMap<>();

    public HandReading(PluginLog log) {
        this.log = log;
    }

    public String material(Player player) {
        try {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null) {
                return EMPTY_HAND;
            }
            return names.computeIfAbsent(held.getType(), HandReading::lowerCaseName);
        } catch (RuntimeException failure) {
            log.debug(LogTopic.DATA, () -> "Main-hand item could not be read: " + failure);
            return UNREADABLE_HAND;
        }
    }

    private static String lowerCaseName(Material type) {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
