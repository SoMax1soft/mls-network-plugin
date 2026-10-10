// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

import java.util.List;

public record OverheadTagConfig(boolean enabled, TagFormats formats, TagReach reach, boolean drawWhenBlank,
                                List<AvgColorBand> bands, HologramDisplay display, TagPlacement placement) {

    public OverheadTagConfig {
        bands = List.copyOf(bands);
    }

    public static List<AvgColorBand> stockShades() {
        return List.of(new AvgColorBand(0, "f", false), new AvgColorBand(60, "c", false),
                new AvgColorBand(90, "4", true));
    }

    public static OverheadTagConfig pluginDefaults() {
        return new OverheadTagConfig(true, TagFormats.stock(), TagReach.stock(), false, stockShades(),
                HologramDisplay.pluginDefaults(), TagPlacement.stock());
    }
}
