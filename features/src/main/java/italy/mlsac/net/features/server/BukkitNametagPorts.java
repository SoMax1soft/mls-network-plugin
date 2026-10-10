// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import italy.mlsac.net.features.live.SuspectRows;
import italy.mlsac.net.features.server.nametag.NametagWire;
import italy.mlsac.net.nametag.NametagPacket;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.nametag.SeedSighting;
import italy.mlsac.net.nametag.TrackingRange;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class BukkitNametagPorts implements NametagPorts {

    private final SuspectRows rows;
    private final PlayerDirectory players;
    private final PacketSender connections;
    private final NametagWire wire;
    private final Supplier<TrackingRange> range;

    public BukkitNametagPorts(SuspectRows rows, PlayerDirectory players, PacketSender connections, NametagWire wire,
                              Supplier<TrackingRange> range) {
        this.rows = rows;
        this.players = players;
        this.connections = connections;
        this.wire = wire;
        this.range = range;
    }

    @Override
    public Optional<HistoryView> history(UUID suspect) {
        return rows.history(suspect);
    }

    @Override
    public long historyRevision(UUID suspect) {
        return rows.historyRevision(suspect);
    }

    @Override
    public boolean staff(UUID viewer) {
        return connections.reachable(viewer)
                && players.byId(viewer).filter(PlayerView::online).map(Audience.STAFF::test).orElse(false);
    }

    @Override
    public List<SeedSighting> currentlyVisibleTo(UUID viewer) {
        Player watcher = Bukkit.getPlayer(viewer);
        if (watcher == null) {
            return List.of();
        }
        World world = watcher.getWorld();
        Location here = watcher.getLocation();
        TrackingRange reach = range.get();
        List<SeedSighting> seen = new ArrayList<>();
        for (Player other : world.getPlayers()) {
            if (other.equals(watcher) || !watcher.canSee(other)) {
                continue;
            }
            Location at = other.getLocation();
            if (reach.covers(at.getX() - here.getX(), at.getZ() - here.getZ())) {
                seen.add(new SeedSighting(other.getEntityId(), at.getX(), at.getY(), at.getZ()));
            }
        }
        return seen;
    }

    @Override
    public Optional<SeedSighting> ownPlace(UUID viewer) {
        Player self = Bukkit.getPlayer(viewer);
        if (self == null) {
            return Optional.empty();
        }
        Location at = self.getLocation();
        return Optional.of(new SeedSighting(self.getEntityId(), at.getX(), at.getY(), at.getZ()));
    }

    @Override
    public void onViewerThread(UUID viewer, Runnable task) {
        connections.onNetworkThread(viewer, task);
    }

    @Override
    public void send(UUID viewer, List<NametagPacket> packets) {
        wire.send(viewer, packets);
    }
}
