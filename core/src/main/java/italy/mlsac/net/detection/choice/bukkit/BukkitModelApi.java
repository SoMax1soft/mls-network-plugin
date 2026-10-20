// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice.bukkit;

import italy.mlsac.net.api.MlsacModelSelection;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.console.PluginLog;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public final class BukkitModelApi {

    private final Plugin plugin;
    private final MlsacModelSelection selection;
    private final ServerEventSink sink;
    private final ApiEventRelay relay;

    public BukkitModelApi(Plugin plugin, EventBus bus, MlsacModelSelection selection, PluginLog log) {
        this.plugin = plugin;
        this.selection = selection;
        this.sink = new ServerEventSink(plugin.getServer().getPluginManager(), log);
        this.relay = new ApiEventRelay(bus, sink);
    }

    public void start() {
        relay.start();
        plugin.getServer().getServicesManager().register(MlsacModelSelection.class, selection, plugin,
                ServicePriority.Normal);
    }

    public void stop() {
        plugin.getServer().getServicesManager().unregister(MlsacModelSelection.class, selection);
        relay.stop();
        sink.close();
    }
}
