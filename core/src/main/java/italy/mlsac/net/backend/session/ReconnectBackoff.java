// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

public final class ReconnectBackoff {

    private static final long FIRST_DELAY_MILLIS = 10_000L;
    private static final long MAX_DELAY_MILLIS = 300_000L;
    private static final int MAX_DOUBLINGS = 5;
    private static final long TICK_LENGTH_MILLIS = 50L;

    public long baseMillis(int attempt) {
        int doublings = Math.min(Math.max(0, attempt), MAX_DOUBLINGS);
        return Math.min(FIRST_DELAY_MILLIS << doublings, MAX_DELAY_MILLIS);
    }

    public long jitterBound(long baseMillis) {
        return baseMillis / 5 + 1;
    }

    public long delayTicks(long delayMillis) {
        return Math.max(1L, delayMillis / TICK_LENGTH_MILLIS);
    }
}
