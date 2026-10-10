// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RejoinVault<T> {

    public static final Duration OWNER_RETENTION = Duration.ofMinutes(15);

    private final long retentionMillis;
    private final Map<UUID, Kept<T>> kept = new ConcurrentHashMap<>();

    public RejoinVault(Duration retention) {
        this.retentionMillis = retention.toMillis();
    }

    public void keep(UUID player, T state, long clockMillis) {
        kept.put(player, new Kept<>(state, clockMillis));
    }

    public Optional<T> reclaim(UUID player, long clockMillis) {
        Kept<T> entry = kept.remove(player);
        if (entry == null || clockMillis - entry.leftAt() > retentionMillis) {
            return Optional.empty();
        }
        return Optional.of(entry.state());
    }

    public Optional<T> peek(UUID player) {
        Kept<T> entry = kept.get(player);
        return entry == null ? Optional.empty() : Optional.of(entry.state());
    }

    public int purgeExpired(long clockMillis) {
        int before = kept.size();
        kept.values().removeIf(entry -> clockMillis - entry.leftAt() > retentionMillis);
        return before - kept.size();
    }

    public void clear() {
        kept.clear();
    }

    public int size() {
        return kept.size();
    }

    private record Kept<T>(T state, long leftAt) {
    }
}
