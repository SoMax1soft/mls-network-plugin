// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class DossierDesk {

    static final long FRESH_MILLIS = 45_000;
    static final long QUIET_MILLIS = 60_000;
    static final int MAX_CACHED = 64;

    private final DossierSource source;
    private final LongSupplier wallMillis;
    private final Map<String, Cached> cache = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<UUID, Boolean> inFlight = new ConcurrentHashMap<>();
    private volatile long quietUntil;

    public DossierDesk(DossierSource source, LongSupplier wallMillis) {
        this.source = source;
        this.wallMillis = wallMillis;
    }

    public CompletableFuture<DossierReply> ask(UUID viewer, DossierQuery query) {
        String key = query.path();
        long now = wallMillis.getAsLong();
        DossierReply cached = fresh(key, now);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        if (now < quietUntil) {
            return CompletableFuture.completedFuture(new DossierReply.Refused(DossierReply.TOO_MANY, "RATE_LIMITED"));
        }
        if (inFlight.putIfAbsent(viewer, Boolean.TRUE) != null) {
            return CompletableFuture.completedFuture(new DossierReply.Busy());
        }
        CompletableFuture<DossierReply> asked;
        try {
            asked = source.fetch(query);
        } catch (RuntimeException failure) {
            inFlight.remove(viewer);
            return CompletableFuture.completedFuture(new DossierReply.Unavailable());
        }
        return asked.handle((reply, error) -> {
            inFlight.remove(viewer);
            DossierReply answer = error == null && reply != null ? reply : new DossierReply.Unavailable();
            remember(key, answer);
            return answer;
        });
    }

    public void forget(UUID viewer) {
        inFlight.remove(viewer);
    }

    private synchronized DossierReply fresh(String key, long now) {
        Cached entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (now - entry.storedAt() >= FRESH_MILLIS) {
            cache.remove(key);
            return null;
        }
        return entry.reply();
    }

    private void remember(String key, DossierReply reply) {
        long now = wallMillis.getAsLong();
        if (reply instanceof DossierReply.Refused refused && refused.throttled()) {
            quietUntil = now + QUIET_MILLIS;
            return;
        }
        if (reply instanceof DossierReply.Found) {
            store(key, reply, now);
        }
    }

    private synchronized void store(String key, DossierReply reply, long now) {
        cache.put(key, new Cached(reply, now));
        while (cache.size() > MAX_CACHED) {
            String eldest = cache.keySet().iterator().next();
            cache.remove(eldest);
        }
    }

    private record Cached(DossierReply reply, long storedAt) {
    }
}
