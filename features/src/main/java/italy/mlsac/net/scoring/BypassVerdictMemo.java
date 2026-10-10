// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class BypassVerdictMemo {

    static final long KEEP_MILLIS = 5_000L;

    private final Predicate<UUID> permissionCheck;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public BypassVerdictMemo(Predicate<UUID> permissionCheck) {
        this.permissionCheck = permissionCheck;
    }

    public boolean bypasses(UUID player, long clockMillis) {
        Entry cached = entries.get(player);
        if (cached != null && clockMillis - cached.askedAtMillis() < KEEP_MILLIS) {
            return cached.bypass();
        }
        boolean bypass = permissionCheck.test(player);
        entries.put(player, new Entry(bypass, clockMillis));
        return bypass;
    }

    public void forget(UUID player) {
        entries.remove(player);
    }

    private record Entry(boolean bypass, long askedAtMillis) {
    }
}
