// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

import java.util.function.LongSupplier;

final class NoticeThrottle {
    private final int perStreak;
    private final long spacingMillis;
    private final LongSupplier wallMillis;
    private int shownThisStreak;
    private long lastShownAt;
    private boolean everShown;

    NoticeThrottle(int perStreak, long spacingMillis, LongSupplier wallMillis) {
        this.perStreak = perStreak;
        this.spacingMillis = spacingMillis;
        this.wallMillis = wallMillis;
    }

    synchronized void endStreak() {
        shownThisStreak = 0;
    }

    synchronized boolean mayShow() {
        long now = wallMillis.getAsLong();
        boolean spaced = !everShown || now - lastShownAt >= spacingMillis;
        if (shownThisStreak >= perStreak || !spaced) {
            return false;
        }
        shownThisStreak++;
        lastShownAt = now;
        everShown = true;
        return true;
    }
}
