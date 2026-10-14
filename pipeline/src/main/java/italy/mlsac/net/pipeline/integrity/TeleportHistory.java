// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class TeleportHistory {
    static final long SETTLE_NANOS = 5_000_000_000L;

    private final Map<UUID, Trail> trails = new ConcurrentHashMap<>();

    void issued(UUID player, int teleportId) {
        if (teleportId >= 0) {
            trails.computeIfAbsent(player, ignored -> new Trail()).issued(teleportId);
        }
    }

    void viewReset(UUID player, long nanos) {
        trails.computeIfAbsent(player, ignored -> new Trail()).reset(nanos);
    }

    ConfirmJudgement judge(UUID player, int teleportId, long nanos) {
        Trail trail = trails.get(player);
        if (teleportId < 0 || trail == null) {
            return ConfirmJudgement.UNVERIFIABLE;
        }
        return trail.judge(teleportId, nanos);
    }

    void forget(UUID player) {
        trails.remove(player);
    }

    void clear() {
        trails.clear();
    }

    private static final class Trail {
        private boolean anyIssued;
        private int highestIssued;
        private boolean resetSeen;
        private long lastResetNanos;

        synchronized void issued(int teleportId) {
            highestIssued = anyIssued ? Math.max(highestIssued, teleportId) : teleportId;
            anyIssued = true;
        }

        synchronized void reset(long nanos) {
            resetSeen = true;
            lastResetNanos = nanos;
        }

        synchronized ConfirmJudgement judge(int teleportId, long nanos) {
            if (!anyIssued || resetSeen && nanos - lastResetNanos < SETTLE_NANOS) {
                return ConfirmJudgement.UNVERIFIABLE;
            }
            return teleportId <= highestIssued ? ConfirmJudgement.ISSUED : ConfirmJudgement.FORGED;
        }
    }
}
