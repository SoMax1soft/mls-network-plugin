// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.api;

import java.util.Locale;
import java.util.Optional;

public enum ModelChoice {
    DEFAULT,
    PRO,
    MINI,
    ULTRA;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ModelChoice> parse(String typed) {
        if (typed == null) {
            return Optional.empty();
        }
        String wanted = typed.trim().toUpperCase(Locale.ROOT);
        for (ModelChoice choice : values()) {
            if (choice.name().equals(wanted)) {
                return Optional.of(choice);
            }
        }
        return Optional.empty();
    }
}
