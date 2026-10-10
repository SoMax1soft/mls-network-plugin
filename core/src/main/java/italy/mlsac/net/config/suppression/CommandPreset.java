// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.Locale;

public enum CommandPreset {
    BAN,
    KICK,
    CUSTOM;

    public static CommandPreset fromIdOrCustom(String raw) {
        String id = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (CommandPreset preset : values()) {
            if (preset.name().equals(id)) {
                return preset;
            }
        }
        return CUSTOM;
    }
}
