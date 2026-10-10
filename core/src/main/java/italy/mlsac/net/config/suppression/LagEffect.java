// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.Locale;
import java.util.Optional;

public enum LagEffect {
    DELAY,
    MISS,
    SETBACK,
    GROUND,
    ROTATE;

    public static Optional<LagEffect> fromId(String raw) {
        String id = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        for (LagEffect effect : values()) {
            if (effect.name().equals(id)) {
                return Optional.of(effect);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
