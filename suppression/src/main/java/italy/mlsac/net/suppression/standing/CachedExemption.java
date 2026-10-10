// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.standing;

import italy.mlsac.net.suppression.server.ExemptionProbe;
import italy.mlsac.net.suppression.time.MonotonicClock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedExemption {

    public static final long MAX_AGE_MILLIS = 5000L;

    private final ExemptionProbe probe;
    private final MonotonicClock clock;
    private final Map<UUID, Verdict> verdicts = new ConcurrentHashMap<>();

    public CachedExemption(ExemptionProbe probe, MonotonicClock clock) {
        this.probe = probe;
        this.clock = clock;
    }

    public boolean exempt(UUID suspectId) {
        long now = clock.clockMillis();
        Verdict cached = verdicts.get(suspectId);
        if (cached != null && now - cached.askedAtMillis() < MAX_AGE_MILLIS) {
            return cached.exempt();
        }
        boolean exempt = probe.exempt(suspectId);
        verdicts.put(suspectId, new Verdict(exempt, now));
        return exempt;
    }

    public void forget(UUID suspectId) {
        verdicts.remove(suspectId);
    }

    public void clear() {
        verdicts.clear();
    }

    private record Verdict(boolean exempt, long askedAtMillis) {
    }
}
