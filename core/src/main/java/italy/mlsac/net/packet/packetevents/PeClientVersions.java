// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.platform.GameVersion;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

final class PeClientVersions implements ClientVersions {

    private final PacketEventsAPI<?> api;

    PeClientVersions(PacketEventsAPI<?> api) {
        this.api = api;
    }

    @Override
    public Optional<GameVersion> version(UUID player) {
        return known(player).flatMap(version -> GameVersion.parse(version.getReleaseName()));
    }

    @Override
    public Optional<String> versionName(UUID player) {
        return known(player).map(ClientVersion::name);
    }

    @Override
    public OptionalInt protocol(UUID player) {
        return known(player).map(version -> OptionalInt.of(version.getProtocolVersion())).orElse(OptionalInt.empty());
    }

    private Optional<ClientVersion> known(UUID player) {
        Object channel = api.getProtocolManager().getChannel(player);
        User user = channel == null ? null : api.getProtocolManager().getUser(channel);
        ClientVersion version = user == null ? null : user.getClientVersion();
        if (version == null || version == ClientVersion.UNKNOWN || version.name().endsWith("_SUPPORTED_VERSIONS")) {
            return Optional.empty();
        }
        return Optional.of(version);
    }
}
