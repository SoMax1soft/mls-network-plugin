// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.integrity;

import italy.mlsac.net.config.ConfigTree;
import java.util.List;
import java.util.Optional;

public final class BadPacketSettingsReader {

    public static final String SECTION = "bad-packets";
    private static final int MAX_COUNT = 1_000;
    private static final int MAX_SECONDS = 3_600;
    private static final int LOWEST_TICK_END_RATE = 25;
    private static final int MAX_TICK_END_RATE = 1_000;
    private static final int LOWEST_TICK_END_BURST = 20;
    private static final int MAX_TICK_END_BURST = 100_000;

    public BadPacketSettings read(ConfigTree file, List<String> warnings) {
        BadPacketSettings defaults = BadPacketSettings.pluginDefaults();
        boolean enabled = file.value(SECTION + ".enabled").filter(Boolean.class::isInstance)
                .map(Boolean.class::cast).orElse(defaults.enabled());
        return new BadPacketSettings(enabled,
                bounded(file, "alert-threshold", defaults.alertThreshold(), 1, MAX_COUNT, warnings),
                bounded(file, "window-seconds", defaults.windowSeconds(), 1, MAX_SECONDS, warnings),
                bounded(file, "strike-interval-seconds", defaults.strikeIntervalSeconds(), 0, MAX_SECONDS, warnings),
                bounded(file, "tick-end-max-per-second", defaults.tickEndsPerSecond(), LOWEST_TICK_END_RATE,
                        MAX_TICK_END_RATE, warnings),
                bounded(file, "tick-end-burst", defaults.tickEndBurst(), LOWEST_TICK_END_BURST, MAX_TICK_END_BURST,
                        warnings));
    }

    private static int bounded(ConfigTree file, String key, int fallback, int min, int max, List<String> warnings) {
        Optional<Object> raw = file.value(SECTION + "." + key);
        if (raw.isEmpty()) {
            return fallback;
        }
        if (!(raw.get() instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            warnings.add(SECTION + "." + key + " must be a number, using " + fallback);
            return fallback;
        }
        long value = Math.round(number.doubleValue());
        if (value < min || value > max) {
            warnings.add(SECTION + "." + key + " must be between " + min + " and " + max + ", using "
                    + Math.max(min, Math.min(max, value)));
        }
        return (int) Math.max(min, Math.min(max, value));
    }
}
