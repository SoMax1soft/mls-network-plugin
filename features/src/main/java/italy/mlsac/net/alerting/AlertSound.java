// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

public record AlertSound(boolean enabled, String type, float volume, float pitch) {

    public static final String DEFAULT_TYPE = "BLOCK_NOTE_BLOCK_PLING";

    public static AlertSound pluginDefaults() {
        return new AlertSound(true, DEFAULT_TYPE, 1.0f, 1.0f);
    }
}
