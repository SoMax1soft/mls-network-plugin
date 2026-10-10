// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.settings;

import italy.mlsac.net.config.hologram.HologramDisplay;
import italy.mlsac.net.config.hologram.OverheadTagConfig;
import italy.mlsac.net.nametag.Billboard;
import italy.mlsac.net.nametag.MeanShadeThreshold;
import italy.mlsac.net.nametag.NametagDisplay;
import italy.mlsac.net.nametag.NametagSettings;
import italy.mlsac.net.nametag.OwnTag;
import italy.mlsac.net.nametag.TagLayout;
import italy.mlsac.net.nametag.TagLimits;
import italy.mlsac.net.nametag.TagTemplates;
import java.util.List;

final class NametagTranslation {

    NametagSettings translate(OverheadTagConfig config) {
        return new NametagSettings(config.enabled(), templates(config), limits(config), config.drawWhenBlank(),
                shades(config), display(config.display()), layout(config));
    }

    private static TagTemplates templates(OverheadTagConfig config) {
        var formats = config.formats();
        return new TagTemplates(formats.lines(), formats.fastItem(), formats.proItem(), formats.ultraItem(),
                formats.pulseItem());
    }

    private static TagLayout layout(OverheadTagConfig config) {
        var placement = config.placement();
        return new TagLayout(placement.extraHeight(), OwnTag.of(placement.showSelf()));
    }

    private static TagLimits limits(OverheadTagConfig config) {
        var reach = config.reach();
        return new TagLimits(reach.pastChecksShown(), reach.seenWithinBlocks());
    }

    private static List<MeanShadeThreshold> shades(OverheadTagConfig config) {
        return config.bands().stream()
                .map(band -> new MeanShadeThreshold(band.from(), band.color(), band.bold()))
                .toList();
    }

    private static NametagDisplay display(HologramDisplay display) {
        return new NametagDisplay(display.background(), display.backgroundRgb(), display.backgroundOpacity(),
                display.shadow(), display.visibleThroughWalls(), display.scale(),
                Billboard.valueOf(display.billboard().name()));
    }
}
