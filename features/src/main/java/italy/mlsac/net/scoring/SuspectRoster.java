// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuspectRoster {

    public static final Duration REJOIN_RETENTION = Duration.ofMinutes(15);

    private final long retentionMillis;
    private final Map<UUID, SuspectRecord> active = new ConcurrentHashMap<>();
    private final Map<UUID, Departed> departed = new ConcurrentHashMap<>();

    public SuspectRoster(Duration retention) {
        this.retentionMillis = retention.toMillis();
    }

    public Optional<SuspectRecord> active(UUID player) {
        return Optional.ofNullable(active.get(player));
    }

    public Optional<SuspectRecord> retained(UUID player) {
        SuspectRecord current = active.get(player);
        if (current != null) {
            return Optional.of(current);
        }
        return Optional.ofNullable(departed.get(player)).map(Departed::record);
    }

    public SuspectRecord admit(UUID player, String name, SuspectKind kind, long clockMillis) {
        return active.computeIfAbsent(player, id -> reclaim(id, name, kind, clockMillis));
    }

    public void depart(UUID player, long clockMillis) {
        SuspectRecord record = active.remove(player);
        if (record != null) {
            departed.put(player, new Departed(record, clockMillis));
        }
    }

    public void forget(UUID player) {
        active.remove(player);
        departed.remove(player);
    }

    public int purgeExpired(long clockMillis) {
        int before = departed.size();
        departed.values().removeIf(entry -> expired(entry, clockMillis));
        return before - departed.size();
    }

    public List<SuspectRecord> activeRecords() {
        return List.copyOf(active.values());
    }

    public void clear() {
        active.clear();
        departed.clear();
    }

    private SuspectRecord reclaim(UUID player, String name, SuspectKind kind, long clockMillis) {
        Departed kept = departed.remove(player);
        if (kept == null || expired(kept, clockMillis) || kept.record().kind() != kind) {
            return new SuspectRecord(player, name, kind);
        }
        kept.record().rename(name);
        return kept.record();
    }

    private boolean expired(Departed entry, long clockMillis) {
        return clockMillis - entry.leftAt() > retentionMillis;
    }

    private record Departed(SuspectRecord record, long leftAt) {
    }
}
