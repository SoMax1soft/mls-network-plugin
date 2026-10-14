// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.message;

import java.util.Locale;
import java.util.Set;

public final class LocalOnlyMessages {

    private static final Set<String> KEYS = Set.of("alert-bad-packets", "alert-format-bad-packets");
    private static final String KEY_FAMILY = "bad-packets-";

    public boolean contains(String key) {
        String normalised = key.toLowerCase(Locale.ROOT);
        return KEYS.contains(normalised) || normalised.startsWith(KEY_FAMILY);
    }
}
