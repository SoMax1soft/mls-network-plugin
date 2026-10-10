// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.detection;

public record DetectionSettings(boolean enabled, RegionSettings regions, ModelRoster models,
                                int legacySequence, int legacyStep) {

    public static DetectionSettings pluginDefaults() {
        return new DetectionSettings(true, RegionSettings.pluginDefaults(), ModelRoster.pluginDefaults(), 40, 10);
    }

    public boolean ultraMode() {
        return models.ultraMode();
    }
}
