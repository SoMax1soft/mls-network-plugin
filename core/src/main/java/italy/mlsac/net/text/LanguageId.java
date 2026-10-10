// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import java.util.Locale;
import java.util.Optional;

public enum LanguageId {
    RU,
    EN,
    UK,
    ES,
    VI,
    ZH,
    AR;

    public static Optional<LanguageId> exact(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String id = raw.trim().toUpperCase(Locale.ROOT);
        for (LanguageId language : values()) {
            if (language.name().equals(id)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    public static LanguageId normalise(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.startsWith("zh") || value.equals("cn")) {
            return ZH;
        }
        if (value.equals("ua")) {
            return UK;
        }
        int dash = value.indexOf('-');
        String base = dash >= 0 ? value.substring(0, dash) : value;
        return exact(base).orElse(EN);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
