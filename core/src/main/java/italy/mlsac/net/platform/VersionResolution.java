// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.List;

public record VersionResolution(String raw, GameVersion version, boolean recognised) {

    public static VersionResolution recognised(String raw, GameVersion version) {
        return new VersionResolution(raw, version, true);
    }

    public static VersionResolution unrecognised(String raw, GameVersion fallback) {
        return new VersionResolution(raw, fallback, false);
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
