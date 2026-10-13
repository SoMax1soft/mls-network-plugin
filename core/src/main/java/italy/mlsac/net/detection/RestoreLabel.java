// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.Locale;
import java.util.Optional;

public enum RestoreLabel {
    LEGIT("LEGIT", 0),
    CHEAT("CHEAT", 1);

    private final String spelling;
    private final int classValue;

    RestoreLabel(String spelling, int classValue) {
        this.spelling = spelling;
        this.classValue = classValue;
    }

    public static Optional<RestoreLabel> parse(String typed) {
        String wanted = typed == null ? "" : typed.trim().toUpperCase(Locale.ROOT);
        for (RestoreLabel label : values()) {
            if (label.spelling.equals(wanted)) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String spelling() {
        return spelling;
    }

    public int classValue() {
        return classValue;
    }
}
