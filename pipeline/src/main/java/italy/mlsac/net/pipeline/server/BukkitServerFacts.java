// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.live.ServerFacts;
import italy.mlsac.net.pipeline.player.ClientVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class BukkitServerFacts implements ServerFacts {

    private final ServerRoster roster;
    private final ClientVersions clients;
    private final PlaceBook places;

    public BukkitServerFacts(ServerRoster roster, ClientVersions clients, PlaceBook places) {
        this.roster = roster;
        this.clients = clients;
        this.places = places;
    }

    @Override
    public boolean valid(UUID player) {
        return roster.player(player).map(Player::isOnline).orElse(false);
    }

    @Override
    public ViewSeed viewSeed(UUID player) {
        Optional<String> versionName = clients.versionName(player);
        if (versionName.isEmpty()) {
            return null;
        }
        String clientVersion = versionName.get();
        Optional<ClientVersion> parsed = ClientVersion.parse(clientVersion);
        if (parsed.isPresent() && parsed.get().below(1, 17, 0)) {
            return versionOnly(clientVersion);
        }
        Optional<PlayerPlace> own = roster.player(player).flatMap(places::of);
        if (own.isEmpty()) {
            return versionOnly(clientVersion);
        }
        List<ViewSeed.SeenPlayer> others = new ArrayList<>();
        for (Player other : roster.online()) {
            places.of(other).map(PlayerPlace::seen).ifPresent(others::add);
        }
        return new ViewSeed(clientVersion, own.get().world(), own.get().position(), List.copyOf(others));
    }

    @Override
    public UUID playerByEntity(int entityNumber) {
        for (Player candidate : roster.online()) {
            if (candidate.getEntityId() == entityNumber) {
                return candidate.getUniqueId();
            }
        }
        return null;
    }

    private static ViewSeed versionOnly(String clientVersion) {
        return new ViewSeed(clientVersion, null, null, List.of());
    }
}
