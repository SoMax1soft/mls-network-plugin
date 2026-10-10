// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.response;

import java.util.Locale;
import java.util.Optional;

public enum TrickKind {
    SHUFFLE_INVENTORY("shuffle_inventory", 1.4, 0.45),
    DROP_WEAPON("drop_weapon", 1.9, 0.55),
    LAUNCH("launch", 1.2, 0.6);

    private final String id;
    private final double defaultHorizontal;
    private final double defaultVertical;

    TrickKind(String id, double defaultHorizontal, double defaultVertical) {
        this.id = id;
        this.defaultHorizontal = defaultHorizontal;
        this.defaultVertical = defaultVertical;
    }

    public static Optional<TrickKind> fromId(String raw) {
        String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (TrickKind kind : values()) {
            if (kind.id.equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return id;
    }

    public double defaultHorizontal() {
        return defaultHorizontal;
    }

    public double defaultVertical() {
        return defaultVertical;
    }
}
