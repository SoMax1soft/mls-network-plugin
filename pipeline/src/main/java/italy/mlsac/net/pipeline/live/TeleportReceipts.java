// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

final class TeleportReceipts {
    private static final int CAPACITY = 32;
    private static final long LIFETIME_NANOS = 30_000_000_000L;
    private final Map<UUID, LinkedHashMap<Integer, Long>> pending = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    TeleportReceipts(LongSupplier clock) {
        this.clock = clock;
    }

    void sent(UUID player, int id) {
        LinkedHashMap<Integer, Long> receipts = pending.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        synchronized (receipts) {
            long now = clock.getAsLong();
            receipts.entrySet().removeIf(entry -> now - entry.getValue() > LIFETIME_NANOS);
            receipts.put(id, now);
            while (receipts.size() > CAPACITY) {
                receipts.remove(receipts.keySet().iterator().next());
            }
        }
    }

    boolean accept(UUID player, int id) {
        LinkedHashMap<Integer, Long> receipts = pending.get(player);
        if (receipts == null) {
            return false;
        }
        synchronized (receipts) {
            Long sentAt = receipts.remove(id);
            return sentAt != null && clock.getAsLong() - sentAt <= LIFETIME_NANOS;
        }
    }

    void forget(UUID player) {
        pending.remove(player);
    }

    void clear() {
        pending.clear();
    }
}
