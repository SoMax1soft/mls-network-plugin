// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.core;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public record PresetSelection(PresetMode mode, String configuredName) {

    public static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");
    private static final Set<String> SENTINELS = Set.of("none", "off", "local", "disabled");

    public static PresetSelection auto() {
        return new PresetSelection(PresetMode.AUTO, "");
    }

    public static ParsedSelection parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return new ParsedSelection(auto(), Optional.empty());
        }
        if (!VALID_NAME.matcher(trimmed).matches()) {
            return new ParsedSelection(auto(), Optional.of("preset name '" + trimmed + "' ignored: "
                    + "allowed are 1 to 32 characters from A-Z, a-z, 0-9, _ and -; the local configuration stays active"));
        }
        PresetMode mode = SENTINELS.contains(trimmed.toLowerCase(Locale.ROOT)) ? PresetMode.DISABLED : PresetMode.NAMED;
        return new ParsedSelection(new PresetSelection(mode, trimmed), Optional.empty());
    }

    public Optional<String> bodyValue() {
        return configuredName.isEmpty() ? Optional.empty() : Optional.of(configuredName);
    }

    public Optional<String> queryName() {
        return mode == PresetMode.NAMED ? Optional.of(configuredName) : Optional.empty();
    }

    public record ParsedSelection(PresetSelection selection, Optional<String> warning) {
    }
}
