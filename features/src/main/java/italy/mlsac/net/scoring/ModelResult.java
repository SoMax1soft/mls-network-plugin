// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Locale;

public record ModelResult(String model, double probability) {

    public static final String UNKNOWN_MODEL = "unknown";

    public static String normaliseModel(String raw) {
        String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return id.isEmpty() ? UNKNOWN_MODEL : id;
    }
}
