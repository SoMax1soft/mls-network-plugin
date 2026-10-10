// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageReductions {

    static final double IGNORED_MULTIPLIER = 0.9999;

    private final Map<UUID, Reduction> active = new ConcurrentHashMap<>();

    public void offer(UUID player, double percent, long expiresAtMillis, long clockMillis) {
        active.compute(player, (id, current) -> {
            if (current == null || current.endsAtMillis() <= clockMillis || percent > current.percent()
                    || expiresAtMillis > current.endsAtMillis()) {
                return new Reduction(percent, expiresAtMillis);
            }
            return current;
        });
    }

    public double multiplier(UUID player, long clockMillis) {
        Reduction reduction = active.get(player);
        if (reduction == null || reduction.endsAtMillis() <= clockMillis) {
            return 1.0;
        }
        double multiplier = Math.max(0.0, 1.0 - reduction.percent() / 100.0);
        return multiplier >= IGNORED_MULTIPLIER ? 1.0 : multiplier;
    }

    public void forget(UUID player) {
        active.remove(player);
    }

    public void clear() {
        active.clear();
    }

    private record Reduction(double percent, long endsAtMillis) {
    }
}
