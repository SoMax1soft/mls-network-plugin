// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.restore;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class RestorePace {
    static final long STAFF_GAP_MILLIS = 5_000;
    static final long TARGET_GAP_MILLIS = 30_000;

    private final LongSupplier wallMillis;
    private final Map<String, Long> staffTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> targetTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> running = new ConcurrentHashMap<>();

    public RestorePace(LongSupplier wallMillis) {
        this.wallMillis = wallMillis;
    }

    synchronized OptionalLong waitSeconds(String staff, UUID target) {
        long now = wallMillis.getAsLong();
        long staffWait = remaining(staffTimes.get(staff), STAFF_GAP_MILLIS, now);
        long targetWait = remaining(targetTimes.get(target), TARGET_GAP_MILLIS, now);
        long wait = Math.max(staffWait, targetWait);
        if (wait > 0) {
            return OptionalLong.of((wait + 999) / 1000);
        }
        staffTimes.put(staff, now);
        targetTimes.put(target, now);
        prune(now);
        return OptionalLong.empty();
    }

    boolean begin(UUID target) {
        return running.putIfAbsent(target, Boolean.TRUE) == null;
    }

    void end(UUID target) {
        running.remove(target);
    }

    private void prune(long now) {
        staffTimes.values().removeIf(time -> now - time >= STAFF_GAP_MILLIS);
        targetTimes.values().removeIf(time -> now - time >= TARGET_GAP_MILLIS);
    }

    private static long remaining(Long last, long gap, long now) {
        return last == null ? 0 : Math.max(0, last + gap - now);
    }
}
