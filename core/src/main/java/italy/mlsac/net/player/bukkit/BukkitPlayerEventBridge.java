// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.player.OnlinePlayerCount;
import italy.mlsac.net.player.PlayerChangedWorld;
import italy.mlsac.net.player.PlayerDied;
import italy.mlsac.net.player.PlayerJoined;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.player.PlayerRespawned;
import italy.mlsac.net.player.PlayerTeleported;
import italy.mlsac.net.player.StandingCache;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

public final class BukkitPlayerEventBridge implements Listener {

    private final Plugin plugin;
    private final EventBus bus;
    private final OnlinePlayerCount online;
    private final StandingCache standings;
    private final Set<UUID> kicked = ConcurrentHashMap.newKeySet();

    public BukkitPlayerEventBridge(Plugin plugin, EventBus bus, OnlinePlayerCount online, StandingCache standings) {
        this.plugin = plugin;
        this.bus = bus;
        this.online = online;
        this.standings = standings;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        online.joined(player.getUniqueId());
        standings.refresh(new BukkitPlayerView(player));
        bus.publish(new PlayerJoined(player.getUniqueId(), player.getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        kicked.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void quitSeen(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        online.left(id);
        bus.publish(new PlayerLeft(id, player.getName(), kicked.remove(id)));
        standings.forget(id);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void deathSeen(PlayerDeathEvent event) {
        bus.publish(new PlayerDied(event.getEntity().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void respawnSeen(PlayerRespawnEvent event) {
        bus.publish(new PlayerRespawned(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void worldChangeSeen(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        bus.publish(new PlayerChangedWorld(player.getUniqueId(), player.getWorld().getUID()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleportSeen(PlayerTeleportEvent event) {
        bus.publish(new PlayerTeleported(event.getPlayer().getUniqueId(), event.getCause().name()));
    }
}
