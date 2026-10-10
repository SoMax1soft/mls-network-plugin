// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.Locale;
import java.util.Optional;

public enum LagLevel {
    LIGHT,
    MEDIUM,
    HEAVY;

    public static Optional<LagLevel> fromId(String raw) {
        String id = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (LagLevel level : values()) {
            if (level.name().equals(id)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
