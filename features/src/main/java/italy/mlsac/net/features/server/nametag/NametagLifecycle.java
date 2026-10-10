// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.nametag;

import italy.mlsac.net.nametag.GameRelease;
import italy.mlsac.net.nametag.NametagDirector;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.nametag.SuspectMode;
import italy.mlsac.net.packet.ClientVersions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class NametagLifecycle implements Listener {

    private final Supplier<Optional<NametagDirector>> directors;
    private final ClientVersions clients;
    private final NametagPorts ports;
    private NametagDirector adopted;

    public NametagLifecycle(Supplier<Optional<NametagDirector>> directors, ClientVersions clients,
                            NametagPorts ports) {
        this.directors = directors;
        this.clients = clients;
        this.ports = ports;
    }

    public synchronized void adoptCurrent() {
        Optional<NametagDirector> current = directors.get();
        if (current.isEmpty() || current.get() == adopted) {
            return;
        }
        NametagDirector director = current.get();
        adopted = director;
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        for (Player player : online) {
            register(director, player);
        }
        for (Player viewer : online) {
            UUID id = viewer.getUniqueId();
            if (ports.staff(id)) {
                director.sight(id).ifPresent(sight -> sight.becomeStaff(ports.currentlyVisibleTo(id)));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void joined(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        current().ifPresent(director -> {
            if (director.sight(player.getUniqueId()).isEmpty()) {
                register(director, player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void left(PlayerQuitEvent event) {
        current().ifPresent(director -> director.left(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void respawned(PlayerRespawnEvent event) {
        renumber(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void switchedWorld(PlayerChangedWorldEvent event) {
        renumber(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void changedMode(PlayerGameModeChangeEvent event) {
        SuspectMode mode = SuspectMode.fromGameMode(event.getNewGameMode().name());
        current().ifPresent(director -> director.modeChanged(event.getPlayer().getUniqueId(), mode));
    }

    private void renumber(Player player) {
        current().ifPresent(director -> director.entityChanged(player.getUniqueId(), player.getEntityId()));
    }

    private Optional<NametagDirector> current() {
        adoptCurrent();
        return directors.get();
    }

    private void register(NametagDirector director, Player player) {
        UUID id = player.getUniqueId();
        Optional<GameRelease> client = clients.version(id)
                .map(version -> new GameRelease(version.major(), version.minor(), version.patch()));
        director.joined(id, player.getEntityId(), client, SuspectMode.fromGameMode(player.getGameMode().name()));
    }
}
