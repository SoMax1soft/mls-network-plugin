// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class BukkitPlayerStates implements PlayerStates {

    private final ServerRoster roster;
    private final ThreadOwnership ownership;
    private final SampleReading reading;
    private final PingReading ping;
    private final PluginLog log;

    public BukkitPlayerStates(ServerRoster roster, ThreadOwnership ownership, SampleReading reading,
                              PingReading ping, PluginLog log) {
        this.roster = roster;
        this.ownership = ownership;
        this.reading = reading;
        this.ping = ping;
        this.log = log;
    }

    @Override
    public List<UUID> connectedPlayers() {
        List<UUID> ids = new ArrayList<>();
        for (Player player : roster.online()) {
            ids.add(player.getUniqueId());
        }
        return ids;
    }

    @Override
    public PlayerSample sample(UUID player) {
        Optional<Player> found = roster.player(player).filter(ownership::ownsEntity);
        if (found.isEmpty()) {
            return null;
        }
        try {
            return reading.read(found.get());
        } catch (RuntimeException failure) {
            log.warn(LogTopic.DATA, "A player state could not be sampled: " + failure.getClass().getSimpleName(),
                    failure);
            return null;
        }
    }

    @Override
    public int serverPing(UUID player) {
        Optional<Player> found = roster.player(player);
        if (found.isEmpty()) {
            return PingReading.ABSENT;
        }
        try {
            return ping.millis(found.get());
        } catch (RuntimeException failure) {
            log.debug(LogTopic.DATA, () -> "Server ping unavailable: " + failure);
            return PingReading.ABSENT;
        }
    }
}
