// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.Locale;
import java.util.Optional;

public enum TagDrawing {
    AUTO("auto"),
    DISPLAY("text display"),
    STAND("armor stand");

    private final String label;

    TagDrawing(String label) {
        this.label = label;
    }

    public static Optional<TagDrawing> parse(String raw) {
        String typed = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (typed) {
            case "auto" -> Optional.of(AUTO);
            case "display" -> Optional.of(DISPLAY);
            case "stand" -> Optional.of(STAND);
            default -> Optional.empty();
        };
    }

    public String label() {
        return label;
    }
}
