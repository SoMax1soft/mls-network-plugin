// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.player.ClientVersion;
import italy.mlsac.net.pipeline.player.VersionNames;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class MovementPrefilter {
    private static final double SAME_SPOT_SQUARED = 1.0E-14;
    private static final ClientVersion FIRST_ECHOING = ClientVersion.of(1, 17, 0);
    private static final ClientVersion FIRST_FIXED = ClientVersion.of(1, 21, 0);

    private final PlayerConnections connections;
    private final VersionNames versions;
    private final Map<UUID, LastPlacement> lastPoints = new ConcurrentHashMap<>();

    MovementPrefilter(PlayerConnections connections, VersionNames versions) {
        this.connections = connections;
        this.versions = versions;
    }

    boolean accepts(MovementPacket packet) {
        if (!packet.carriesPosition() || packet.cancelled()) {
            return true;
        }
        UUID player = packet.player();
        LastPlacement last = lastPoints.get(player);
        boolean echo = packet.carriesRotation() && echoesPlacement(player) && last != null && last.repeatedBy(packet);
        if (!echo) {
            if (last == null) {
                lastPoints.put(player, new LastPlacement(packet));
            } else {
                last.remember(packet);
            }
        }
        return !echo;
    }

    void forget(UUID player) {
        lastPoints.remove(player);
    }

    private boolean echoesPlacement(UUID player) {
        Optional<ClientVersion> version = versions.parse(connections.clientProtocol(player));
        return version.isPresent() && version.get().compareTo(FIRST_ECHOING) >= 0
                && version.get().compareTo(FIRST_FIXED) < 0;
    }

    private static final class LastPlacement {
        private double x;
        private double y;
        private double z;
        private boolean grounded;

        LastPlacement(MovementPacket packet) {
            remember(packet);
        }

        void remember(MovementPacket packet) {
            x = packet.x();
            y = packet.y();
            z = packet.z();
            grounded = packet.grounded();
        }

        boolean repeatedBy(MovementPacket packet) {
            if (grounded != packet.grounded()) {
                return false;
            }
            double dx = x - packet.x();
            double dy = y - packet.y();
            double dz = z - packet.z();
            return dx * dx + dy * dy + dz * dz < SAME_SPOT_SQUARED;
        }
    }
}
