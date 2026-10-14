// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class TickEndBudget {
    static final long REPORT_EVERY_NANOS = 1_000_000_000L;

    private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    TickEndSpend spend(UUID player, long nanos, TickEndAllowance allowance) {
        return buckets.computeIfAbsent(player, ignored -> new Bucket(nanos, allowance.burst()))
                .spend(nanos, allowance);
    }

    void forget(UUID player) {
        buckets.remove(player);
    }

    void clear() {
        buckets.clear();
    }

    private static final class Bucket {
        private double tokens;
        private long refilledAt;
        private boolean reported;
        private long reportedAt;

        Bucket(long nanos, double tokens) {
            this.tokens = tokens;
            this.refilledAt = nanos;
        }

        synchronized TickEndSpend spend(long nanos, TickEndAllowance allowance) {
            if (nanos > refilledAt) {
                tokens = Math.min(allowance.burst(), tokens + (nanos - refilledAt) / 1e9 * allowance.perSecond());
                refilledAt = nanos;
            }
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return TickEndSpend.WITHIN_BUDGET;
            }
            if (reported && nanos - reportedAt < REPORT_EVERY_NANOS) {
                return TickEndSpend.EXCESS;
            }
            reported = true;
            reportedAt = nanos;
            return TickEndSpend.EXCESS_REPORTED;
        }
    }
}
