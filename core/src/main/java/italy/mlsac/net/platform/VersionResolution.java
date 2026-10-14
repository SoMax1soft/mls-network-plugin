// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VersionResolution(String raw, GameVersion version, boolean recognised) {

    private static final Pattern NUMERIC_PREFIX = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)(?!\\d|\\.\\d)");

    public static VersionResolution recognised(String raw, GameVersion version) {
        return new VersionResolution(raw, version, true);
    }

    public static VersionResolution unrecognised(String raw, GameVersion fallback) {
        return new VersionResolution(raw, fallback, false);
    }

    static Optional<String> numericPrefix(String raw) {
        Matcher prefix = NUMERIC_PREFIX.matcher(raw == null ? "" : raw);
        return prefix.find() ? Optional.of(prefix.group(1)) : Optional.empty();
    }

    public String detected() {
        return numericPrefix(raw).orElseGet(() -> raw == null || raw.isBlank() ? version.toString() : raw.trim());
    }

    public CompatibilityMode mode() {
        return CompatibilityMode.of(version);
    }

    public List<String> warnings() {
        if (recognised) {
            return List.of();
        }
        return List.of("Unrecognised server version '" + raw + "', using the 1.16.5 rules");
    }
}
