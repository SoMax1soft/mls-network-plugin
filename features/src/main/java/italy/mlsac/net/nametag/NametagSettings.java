// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public record NametagSettings(boolean enabled, TagTemplates templates, TagLimits limits, boolean drawWhenBlank,
        List<MeanShadeThreshold> bands, NametagDisplay display, TagLayout layout) {

    public NametagSettings {
        bands = bands.isEmpty() ? MeanShadeThreshold.defaults() : List.copyOf(bands);
    }

    public static NametagSettings pluginDefaults() {
        return new NametagSettings(true, TagTemplates.stock(), TagLimits.stock(), false, MeanShadeThreshold.defaults(),
                NametagDisplay.pluginDefaults(), TagLayout.stock());
    }

    public NametagSettings switchedOn(boolean on) {
        return new NametagSettings(on, templates, limits, drawWhenBlank, bands, display, layout);
    }

    public NametagSettings blankDrawn(boolean drawn) {
        return new NametagSettings(enabled, templates, limits, drawn, bands, display, layout);
    }

    public NametagSettings retemplated(TagTemplates replacement) {
        return new NametagSettings(enabled, replacement, limits, drawWhenBlank, bands, display, layout);
    }

    public NametagSettings redisplayed(NametagDisplay replacement) {
        return new NametagSettings(enabled, templates, limits, drawWhenBlank, bands, replacement, layout);
    }

    public NametagSettings relaid(TagLayout replacement) {
        return new NametagSettings(enabled, templates, limits, drawWhenBlank, bands, display, replacement);
    }

    public boolean sameLook(NametagSettings other) {
        return display.equals(other.display) && limits.seenWithinBlocks() == other.limits.seenWithinBlocks()
                && layout.extraHeight() == other.layout.extraHeight();
    }
}
