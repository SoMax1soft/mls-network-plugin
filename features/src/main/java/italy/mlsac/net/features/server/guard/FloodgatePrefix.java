// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.guard;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.guard.BedrockPrefix;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class FloodgatePrefix implements BedrockPrefix {

    static final String FALLBACK = ".";
    private static final String PLUGIN = "floodgate";
    private static final String API = "org.geysermc.floodgate.api.FloodgateApi";

    private final PluginLog log;
    private volatile Optional<String> known;

    public FloodgatePrefix(PluginLog log) {
        this.log = log;
    }

    @Override
    public Optional<String> prefix() {
        Optional<String> cached = known;
        if (cached != null) {
            return cached;
        }
        if (Bukkit.getServer() == null) {
            return Optional.empty();
        }
        Plugin floodgate = Bukkit.getPluginManager().getPlugin(PLUGIN);
        Optional<String> found = floodgate == null || !floodgate.isEnabled() ? Optional.empty() : read(floodgate);
        if (found.isPresent()) {
            known = found;
        }
        return found;
    }

    private Optional<String> read(Plugin floodgate) {
        try {
            Class<?> api = floodgate.getClass().getClassLoader().loadClass(API);
            Object instance = api.getMethod("getInstance").invoke(null);
            Object prefix = api.getMethod("getPlayerPrefix").invoke(instance);
            return Optional.of(prefix == null ? FALLBACK : String.valueOf(prefix));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            log.debug(LogTopic.PENALTY, () -> "Floodgate prefix not readable, assuming '" + FALLBACK + "': " + failure);
            return Optional.of(FALLBACK);
        }
    }
}
