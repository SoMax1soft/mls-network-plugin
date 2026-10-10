// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

import italy.mlsac.net.config.CleanText;
import italy.mlsac.net.config.SettingsLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class HologramReader {

    private static final Pattern RGB = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern BAND_COLOR = Pattern.compile("^([0-9a-f]|#[0-9a-fA-F]{6})$");
    private static final int MAX_FORMAT = 128;
    private static final int MAX_BANDS = 6;
    private static final int MAX_LINES = 4;
    private static final String BANDS = "holograms." + "avg-colors";
    private static final String LINES = "holograms.lines";

    public OverheadTagConfig read(OverheadTagConfig lower, SettingsLayer layer) {
        return new OverheadTagConfig(
                layer.bool("holograms.enabled").orElse(lower.enabled()),
                readFormats(lower.formats(), layer),
                readReach(lower.reach(), layer),
                layer.bool("holograms.show-empty").orElse(lower.drawWhenBlank()),
                readBands(layer).orElse(lower.bands()),
                readDisplay(lower.display(), layer),
                readPlacement(lower.placement(), layer));
    }

    private static TagFormats readFormats(TagFormats lower, SettingsLayer layer) {
        List<String> lines = readLines(layer)
                .or(() -> layer.text("holograms.format", MAX_FORMAT).map(List::of))
                .orElse(lower.lines());
        return new TagFormats(
                lines,
                layer.text("holograms.fast-format", MAX_FORMAT).orElse(lower.fastItem()),
                layer.text("holograms.pro-format", MAX_FORMAT).orElse(lower.proItem()),
                layer.text("holograms.ultra-format", MAX_FORMAT).orElse(lower.ultraItem()),
                layer.text("holograms.pulse-format", MAX_FORMAT).orElse(lower.pulseItem()));
    }

    private static TagReach readReach(TagReach lower, SettingsLayer layer) {
        return new TagReach(
                layer.intClamped("holograms.history-limit", 1, 20).orElse(lower.pastChecksShown()),
                layer.intClamped("holograms.view-distance", 8, 128).orElse(lower.seenWithinBlocks()));
    }

    private static Optional<List<String>> readLines(SettingsLayer layer) {
        Optional<List<?>> list = layer.list(LINES);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < list.get().size(); i++) {
            Object raw = list.get().get(i);
            String label = LINES + '[' + i + ']';
            if (!(raw instanceof String text)) {
                layer.drop(label, "not a string");
            } else if (lines.size() >= MAX_LINES) {
                layer.drop(label, "more than " + MAX_LINES + " lines");
            } else {
                lines.add(CleanText.of(text, MAX_FORMAT));
            }
        }
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        layer.accepted();
        return Optional.of(List.copyOf(lines));
    }

    private static TagPlacement readPlacement(TagPlacement lower, SettingsLayer layer) {
        return new TagPlacement(
                layer.clamped("holograms.height", -1.0, 3.0).orElse(lower.extraHeight()),
                layer.bool("holograms.show-self").orElse(lower.showSelf()));
    }

    private HologramDisplay readDisplay(HologramDisplay lower, SettingsLayer layer) {
        int rgb = layer.matching("holograms.display.background-color", RGB)
                .map(hex -> Integer.parseInt(hex.substring(1), 16))
                .orElse(lower.backgroundRgb());
        return new HologramDisplay(
                layer.bool("holograms.display.background").orElse(lower.background()),
                rgb,
                layer.intClamped("holograms.display.background-opacity", 0, 100).orElse(lower.backgroundOpacity()),
                layer.bool("holograms.display.shadow").orElse(lower.shadow()),
                layer.bool("holograms.display.see-through").orElse(lower.visibleThroughWalls()),
                layer.clamped("holograms.display.scale", 0.5, 2.0).orElse(lower.scale()),
                layer.text("holograms.display.billboard", 16).flatMap(Billboard::fromId).orElse(lower.billboard()));
    }

    private Optional<List<AvgColorBand>> readBands(SettingsLayer layer) {
        Optional<List<?>> list = layer.list(BANDS);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        List<AvgColorBand> bands = new ArrayList<>();
        for (int i = 0; i < list.get().size() && bands.size() < MAX_BANDS; i++) {
            Object raw = list.get().get(i);
            String label = BANDS + '[' + i + ']';
            if (raw instanceof Map<?, ?> map) {
                readBand(layer.element(map, label)).ifPresent(bands::add);
            } else {
                layer.drop(label, "not an object");
            }
        }
        List<AvgColorBand> ascending = strictlyAscending(bands);
        return Optional.of(ascending.isEmpty() ? OverheadTagConfig.stockShades() : ascending);
    }

    private static Optional<AvgColorBand> readBand(SettingsLayer band) {
        OptionalInt from = band.intClamped("from", 0, 100);
        Optional<String> color = band.matching("color", BAND_COLOR);
        if (from.isEmpty() || color.isEmpty()) {
            band.drop("", "band needs numeric from and a valid color");
            return Optional.empty();
        }
        return Optional.of(new AvgColorBand(from.getAsInt(), color.get(), band.bool("bold").orElse(false)));
    }

    private static List<AvgColorBand> strictlyAscending(List<AvgColorBand> bands) {
        List<AvgColorBand> kept = new ArrayList<>();
        for (AvgColorBand band : bands) {
            if (kept.isEmpty() || band.from() > kept.get(kept.size() - 1).from()) {
                kept.add(band);
            }
        }
        return kept;
    }
}
