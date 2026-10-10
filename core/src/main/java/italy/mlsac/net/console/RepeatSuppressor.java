// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import italy.mlsac.net.clock.Clock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

final class RepeatSuppressor {

    static final long WINDOW_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_TRACKED = 512;

    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    RepeatSuppressor(Clock clock) {
        this.clock = clock;
    }

    synchronized Optional<String> admit(String line) {
        long now = clock.wallMillis("log.repeat");
        String key = withoutDigits(line);
        Window window = windows.get(key);
        if (window != null && now - window.openedAt < WINDOW_MILLIS) {
            window.suppressed++;
            return Optional.empty();
        }
        int repeated = window == null ? 0 : window.suppressed;
        if (windows.size() >= MAX_TRACKED) {
            evictExpired(now);
        }
        windows.put(key, new Window(now));
        return Optional.of(repeated > 0 ? line + " (repeated " + repeated + " times)" : line);
    }

    private void evictExpired(long now) {
        Iterator<Window> it = windows.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().openedAt >= WINDOW_MILLIS) {
                it.remove();
            }
        }
        if (windows.size() >= MAX_TRACKED) {
            windows.clear();
        }
    }

    static String withoutDigits(String line) {
        StringBuilder out = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c < '0' || c > '9') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static final class Window {
        final long openedAt;
        int suppressed;

        Window(long openedAt) {
            this.openedAt = openedAt;
        }
    }
}
