// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.lifecycle.bukkit;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.lifecycle.TickSource;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class PaperTickEndSource implements TickSource, Listener {

    private final Plugin plugin;
    private final EventBus bus;
    private long tick;

    public PaperTickEndSource(Plugin plugin, EventBus bus) {
        this.plugin = plugin;
        this.bus = bus;
    }

    @Override
    public void start() {
        tick = 0;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void tickFinished(ServerTickEndEvent event) {
        bus.publish(new ServerTick(++tick));
    }
}
