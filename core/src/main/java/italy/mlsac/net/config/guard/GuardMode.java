// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.guard;

import java.util.Locale;
import java.util.Optional;

public enum GuardMode {
    BLOCK,
    WARN;

    public static Optional<GuardMode> parse(String raw) {
        String id = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (GuardMode mode : values()) {
            if (mode.name().equals(id)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    public boolean dispatchesRefused() {
        return this == WARN;
    }
}
