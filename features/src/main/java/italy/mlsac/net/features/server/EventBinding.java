// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public interface EventBinding extends Listener {

    void bindTo(Plugin plugin, PluginManager events);
}
