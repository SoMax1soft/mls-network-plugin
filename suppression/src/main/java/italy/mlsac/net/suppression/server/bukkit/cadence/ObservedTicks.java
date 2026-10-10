// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.player.PlayerLeft;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ObservedTicks {

    private final AtomicLong serverTicks = new AtomicLong();
    private final Map<UUID, AtomicLong> playerTicks = new ConcurrentHashMap<>();

    public void serverTicked(ServerTick tick) {
        serverTicks.set(tick.tick());
    }

    public void playerTicked(PlayerTick tick) {
        playerTicks.computeIfAbsent(tick.player(), id -> new AtomicLong()).set(tick.tick());
    }

    public void playerLeft(PlayerLeft left) {
        playerTicks.remove(left.player());
    }

    public long observedTickOf(UUID suspectId) {
        AtomicLong own = suspectId == null ? null : playerTicks.get(suspectId);
        return (own == null ? serverTicks.get() : own.get()) + 1L;
    }
}
