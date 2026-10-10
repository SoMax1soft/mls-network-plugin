// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.module;

import java.util.Optional;
import org.bukkit.plugin.Plugin;

public final class HostHandles {

    private final Plugin plugin;

    private HostHandles(Plugin plugin) {
        this.plugin = plugin;
    }

    public static HostHandles of(Plugin plugin) {
        return new HostHandles(plugin);
    }

    public static HostHandles detached() {
        return new HostHandles(null);
    }

    public Optional<Plugin> plugin() {
        return Optional.ofNullable(plugin);
    }
}
