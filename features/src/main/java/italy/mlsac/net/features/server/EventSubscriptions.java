// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

final class EventSubscriptions {

    private final List<Listener> registered = new ArrayList<>();

    synchronized void register(Plugin plugin, Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        registered.add(listener);
    }

    synchronized void attach(Plugin plugin, EventBinding binding) {
        binding.bindTo(plugin, Bukkit.getPluginManager());
        registered.add(binding);
    }

    synchronized void unregisterAll() {
        registered.forEach(HandlerList::unregisterAll);
        registered.clear();
    }
}
