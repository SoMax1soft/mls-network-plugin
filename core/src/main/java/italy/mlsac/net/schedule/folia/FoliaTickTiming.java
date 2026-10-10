// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule.folia;

public final class FoliaTickTiming {

    private static final long TICK_LENGTH_MILLIS = 50L;

    public long delayTicks(long requested) {
        return requested <= 0 ? 1L : requested;
    }

    public long everyTicks(long requested) {
        return requested <= 0 ? 1L : requested;
    }

    public long asyncMillis(long ticks) {
        return ticks <= 0 ? TICK_LENGTH_MILLIS : ticks * TICK_LENGTH_MILLIS;
    }
}
