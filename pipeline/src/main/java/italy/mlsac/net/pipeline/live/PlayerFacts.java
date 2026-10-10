// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.player.StandingCache;
import italy.mlsac.net.region.RegionExemptions;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class PlayerFacts implements PlayerStanding, PlayerConnections {

    private static final String UNKNOWN_CLIENT = "unknown";

    private final StandingCache standings;
    private final RegionExemptions regions;
    private final AnimatingPlayers animations;
    private final ClientVersions clients;
    private final PlayerDirectory players;
    private final ServerFacts server;

    public PlayerFacts(StandingCache standings, RegionExemptions regions, AnimatingPlayers animations,
                       ClientVersions clients, PlayerDirectory players, ServerFacts server) {
        this.standings = standings;
        this.regions = regions;
        this.animations = animations;
        this.clients = clients;
        this.players = players;
        this.server = server;
    }

    @Override
    public boolean valid(UUID player) {
        return server.valid(player);
    }

    @Override
    public boolean exempt(UUID player) {
        return standings.standing(player).bypass();
    }

    @Override
    public boolean bedrock(UUID player) {
        return standings.standing(player).bedrock();
    }

    @Override
    public boolean zoneExcluded(UUID player) {
        return regions.lastKnown(player);
    }

    @Override
    public boolean animating(UUID player) {
        return animations.animating(player);
    }

    @Override
    public String clientProtocol(UUID player) {
        return clients.versionName(player).orElse(null);
    }

    @Override
    public String clientLabel(UUID player) {
        Optional<GameVersion> version = clients.version(player);
        OptionalInt protocol = clients.protocol(player);
        if (version.isEmpty() || protocol.isEmpty()) {
            return UNKNOWN_CLIENT;
        }
        return version.get() + " (protocol " + protocol.getAsInt() + ")";
    }

    @Override
    public ViewSeed viewSeed(UUID player) {
        return server.viewSeed(player);
    }

    @Override
    public UUID playerByEntity(int entityNumber) {
        return server.playerByEntity(entityNumber);
    }

    @Override
    public String name(UUID player) {
        return players.byId(player).map(PlayerView::name).orElse(player.toString());
    }
}
