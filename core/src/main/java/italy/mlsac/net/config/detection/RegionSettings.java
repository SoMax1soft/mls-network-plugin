// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.detection;

import java.util.List;

public record RegionSettings(boolean enabled, List<RegionEntry> entries) {

    public RegionSettings {
        entries = List.copyOf(entries);
    }

    public static RegionSettings pluginDefaults() {
        return new RegionSettings(true, List.of());
    }

    public boolean needsRegionLookup() {
        return enabled && entries.stream().anyMatch(RegionEntry::needsRegionLookup);
    }

    public RegionSettings withEnabled(boolean value) {
        return new RegionSettings(value, entries);
    }

    public RegionSettings withEntries(List<RegionEntry> value) {
        return new RegionSettings(enabled, value);
    }
}
