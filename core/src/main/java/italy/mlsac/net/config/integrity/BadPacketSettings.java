// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.integrity;

public record BadPacketSettings(boolean enabled, int alertThreshold, int windowSeconds, int strikeIntervalSeconds,
                                int tickEndsPerSecond, int tickEndBurst) {

    public static final int DEFAULT_ALERT_THRESHOLD = 3;
    public static final int DEFAULT_WINDOW_SECONDS = 60;
    public static final int DEFAULT_STRIKE_INTERVAL_SECONDS = 10;
    public static final int DEFAULT_TICK_ENDS_PER_SECOND = 30;
    public static final int DEFAULT_TICK_END_BURST = 200;

    public static BadPacketSettings pluginDefaults() {
        return new BadPacketSettings(true, DEFAULT_ALERT_THRESHOLD, DEFAULT_WINDOW_SECONDS,
                DEFAULT_STRIKE_INTERVAL_SECONDS, DEFAULT_TICK_ENDS_PER_SECOND, DEFAULT_TICK_END_BURST);
    }

    public long windowMillis() {
        return windowSeconds * 1000L;
    }

    public long strikeIntervalMillis() {
        return strikeIntervalSeconds * 1000L;
    }
}
