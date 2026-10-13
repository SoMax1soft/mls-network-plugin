// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.staff;

public record DataRestoreSettings(boolean enabled, int maxPlayers, int maxMegabytes) {

    public static final int DEFAULT_MAX_PLAYERS = 40;
    public static final int DEFAULT_MAX_MEGABYTES = 64;

    public static DataRestoreSettings pluginDefaults() {
        return new DataRestoreSettings(false, DEFAULT_MAX_PLAYERS, DEFAULT_MAX_MEGABYTES);
    }

    public long maxBytes() {
        return maxMegabytes * 1024L * 1024L;
    }
}
