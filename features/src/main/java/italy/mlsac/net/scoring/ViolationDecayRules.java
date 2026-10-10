// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public record ViolationDecayRules(boolean enabled, int everySeconds, int amount) {

    public static final int TICKS_PER_SECOND = 20;

    public static ViolationDecayRules pluginDefaults() {
        return new ViolationDecayRules(false, 1500, 1);
    }

    public long everyTicks() {
        return (long) everySeconds * TICKS_PER_SECOND;
    }
}
