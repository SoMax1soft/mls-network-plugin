// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import italy.mlsac.net.clock.Clock;
import java.util.HashMap;
import java.util.Map;

final class TraceThrottle {

    private static final int MAX_TRACKED = 256;

    private final Clock clock;
    private final Map<String, Long> lastTrace = new HashMap<>();

    TraceThrottle(Clock clock) {
        this.clock = clock;
    }

    synchronized boolean allow(Throwable cause) {
        String key = causeKey(cause);
        long now = clock.wallMillis("log.trace");
        Long last = lastTrace.get(key);
        if (last != null && now - last < RepeatSuppressor.WINDOW_MILLIS) {
            return false;
        }
        if (lastTrace.size() > MAX_TRACKED) {
            lastTrace.clear();
        }
        lastTrace.put(key, now);
        return true;
    }

    private static String causeKey(Throwable cause) {
        StackTraceElement[] frames = cause.getStackTrace();
        String top = frames.length > 0 ? frames[0].toString() : "";
        return cause.getClass().getName() + " at " + top;
    }
}
