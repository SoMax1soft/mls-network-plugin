// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.nametag;

import italy.mlsac.net.nametag.TrackingRange;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public final class SpigotTrackingRange implements Supplier<TrackingRange> {

    static final String PLAYERS_RANGE = "world-settings.default.entity-tracking-range.players";

    private volatile TrackingRange read;

    @Override
    public TrackingRange get() {
        TrackingRange current = read;
        if (current == null) {
            current = readSetting();
            read = current;
        }
        return current;
    }

    public static TrackingRange from(ConfigurationSection spigotSettings) {
        if (spigotSettings == null) {
            return TrackingRange.whenUnreadable();
        }
        return TrackingRange.fromSpigotSetting(spigotSettings.isInt(PLAYERS_RANGE)
                ? OptionalInt.of(spigotSettings.getInt(PLAYERS_RANGE)) : OptionalInt.empty());
    }

    private static TrackingRange readSetting() {
        try {
            return from(Bukkit.spigot().getConfig());
        } catch (RuntimeException | LinkageError unreadable) {
            return TrackingRange.whenUnreadable();
        }
    }
}
