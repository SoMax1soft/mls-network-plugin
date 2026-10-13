// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class RepeatGate {

    public static final long QUIET_MILLIS = 5L * 60_000L;
    private static final int MAX_KEYS = 512;

    private final Map<String, Window> windows = new HashMap<>();

    public synchronized OptionalInt admit(String key, long now) {
        Window window = windows.get(key);
        if (window != null && now - window.openedAt() < QUIET_MILLIS) {
            windows.put(key, new Window(window.openedAt(), window.held() + 1));
            return OptionalInt.empty();
        }
        if (windows.size() >= MAX_KEYS) {
            windows.values().removeIf(open -> now - open.openedAt() >= QUIET_MILLIS);
        }
        windows.put(key, new Window(now, 0));
        return OptionalInt.of(window == null ? 0 : window.held());
    }

    private record Window(long openedAt, int held) {
    }
}
