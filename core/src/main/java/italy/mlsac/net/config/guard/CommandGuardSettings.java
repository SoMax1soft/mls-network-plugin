// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.guard;

import java.util.Objects;
import java.util.Set;

public record CommandGuardSettings(boolean enabled, GuardMode mode, Set<String> extraAllowedRoots,
                                   int maxCommandsPerMinute, int maxCommandsPerPlayerPerMinute,
                                   int maxSameCommandBurst, int burstPauseMinutes) {

    public static final int DEFAULT_MAX_PER_MINUTE = 120;
    public static final int DEFAULT_MAX_PER_PLAYER_PER_MINUTE = 30;
    public static final int DEFAULT_MAX_SAME_COMMAND_BURST = 15;
    public static final int DEFAULT_BURST_PAUSE_MINUTES = 10;

    public CommandGuardSettings {
        Objects.requireNonNull(mode, "mode");
        extraAllowedRoots = Set.copyOf(extraAllowedRoots);
    }

    public static CommandGuardSettings pluginDefaults() {
        return new CommandGuardSettings(true, GuardMode.BLOCK, Set.of(), DEFAULT_MAX_PER_MINUTE,
                DEFAULT_MAX_PER_PLAYER_PER_MINUTE, DEFAULT_MAX_SAME_COMMAND_BURST, DEFAULT_BURST_PAUSE_MINUTES);
    }
}
