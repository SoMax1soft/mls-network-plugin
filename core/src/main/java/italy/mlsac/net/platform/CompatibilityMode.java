// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

public enum CompatibilityMode {
    MODERN("Modern (1.20.5+)"),
    LEGACY_MODERN("Legacy-Modern (1.17-1.20.4)"),
    LEGACY("Legacy (1.16.x)");

    private static final GameVersion MODERN_FROM = GameVersion.of(1, 20, 5);
    private static final GameVersion LEGACY_MODERN_FROM = GameVersion.of(1, 17, 0);

    private final String label;

    CompatibilityMode(String label) {
        this.label = label;
    }

    public static CompatibilityMode of(GameVersion version) {
        if (version.atLeast(MODERN_FROM)) {
            return MODERN;
        }
        return version.atLeast(LEGACY_MODERN_FROM) ? LEGACY_MODERN : LEGACY;
    }

    public String label() {
        return label;
    }
}
