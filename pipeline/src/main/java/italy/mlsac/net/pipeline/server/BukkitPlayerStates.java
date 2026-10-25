// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.turn.PlayerTurn;
import italy.mlsac.net.pipeline.turn.TurnPart;
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
    private volatile Resolved lastSampled;

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
        return sample(player, true);
    }

    @Override
    public PlayerSample sample(UUID player, boolean complete) {
        Optional<Player> found = roster.player(player);
        return found.isEmpty() ? null : sample(player, new ServerPlayerSlot(found.get()), complete);
    }

    @Override
    public PlayerSample sample(UUID player, boolean complete, PlayerTurn turn) {
        ServerPlayerSlot slot = (ServerPlayerSlot) turn.part(TurnPart.SERVER_PLAYER);
        if (slot == null) {
            Optional<Player> found = roster.player(player);
            if (found.isEmpty()) {
                return null;
            }
            slot = new ServerPlayerSlot(found.get());
            turn.keep(TurnPart.SERVER_PLAYER, slot);
        }
        return sample(player, slot, complete);
    }

    private PlayerSample sample(UUID player, ServerPlayerSlot slot, boolean complete) {
        if (!ownership.ownsEntity(slot.player())) {
            return null;
        }
        lastSampled = new Resolved(Thread.currentThread(), player, slot.player());
        try {
            return reading.read(slot, complete);
        } catch (RuntimeException failure) {
            log.warn(LogTopic.DATA, "A player state could not be sampled: " + failure.getClass().getSimpleName(),
                    failure);
            return null;
        }
    }

    @Override
    public int serverPing(UUID player) {
        return pingOf(roster.player(player));
    }

    @Override
    public int serverPingAfterSample(UUID player) {
        Resolved resolved = lastSampled;
        if (resolved == null || resolved.thread() != Thread.currentThread() || !resolved.player().equals(player)) {
            return serverPing(player);
        }
        lastSampled = null;
        return pingOf(Optional.of(resolved.handle()));
    }

    private int pingOf(Optional<Player> found) {
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

    private record Resolved(Thread thread, UUID player, Player handle) {
    }
}
