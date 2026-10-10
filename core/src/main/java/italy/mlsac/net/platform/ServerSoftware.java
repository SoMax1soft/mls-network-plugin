// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

public enum ServerSoftware {
    FOLIA("Folia"),
    PAPER("Paper"),
    SPIGOT("Spigot/Bukkit");

    static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";
    static final String PAPER_LEGACY_MARKER = "com.destroystokyo.paper.PaperConfig";
    static final String PAPER_MODERN_MARKER = "io.papermc.paper.configuration.Configuration";

    private final String displayName;

    ServerSoftware(String displayName) {
        this.displayName = displayName;
    }

    public static ServerSoftware detect(ClassProbe probe) {
        if (probe.isPresent(FOLIA_MARKER)) {
            return FOLIA;
        }
        if (probe.isPresent(PAPER_LEGACY_MARKER) || probe.isPresent(PAPER_MODERN_MARKER)) {
            return PAPER;
        }
        return SPIGOT;
    }

    public String displayName() {
        return displayName;
    }
}
