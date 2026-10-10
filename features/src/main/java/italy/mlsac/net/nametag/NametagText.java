// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.scoring.ModelResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class NametagText {

    public static final String EMPTY_LINE = "&7no average &8/ &7no history yet";
    static final String ITEM_UNSEEN = "?";

    private final HexColorNormalizer colors;

    public NametagText(HexColorNormalizer colors) {
        this.colors = colors;
    }

    public Optional<TagLines> build(List<ModelResult> history, NametagSettings settings) {
        if (!settings.enabled()) {
            return Optional.empty();
        }
        if (history.isEmpty()) {
            return settings.drawWhenBlank() ? Optional.of(TagLines.single(colors.normalise(EMPTY_LINE)))
                    : Optional.empty();
        }
        List<ModelResult> shown = history.subList(Math.max(0, history.size() - settings.limits().pastChecksShown()), history.size());
        long average = Math.round(shown.stream().mapToDouble(ModelResult::probability).average().orElse(0) * 100.0);
        String averageText = MeanShadeThreshold.forValue(settings.bands(), average).prefix() + average;
        String items = items(shown, settings);
        List<String> lines = settings.templates().lines().stream()
                .map(template -> colors.normalise(template.replace("{AVG}", averageText).replace("{HIST}", items)))
                .toList();
        return Optional.of(new TagLines(lines));
    }

    private static String items(List<ModelResult> shown, NametagSettings settings) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < shown.size(); index++) {
            if (index > 0) {
                items.append(' ');
            }
            items.append(item(shown.get(index), settings));
        }
        return items.toString();
    }

    private static String item(ModelResult result, NametagSettings settings) {
        return ModelFamily.of(result.model())
                .map(family -> family.format(settings).replace("{RESULT}", Long.toString((long) (result.probability() * 100.0))))
                .orElse(ITEM_UNSEEN);
    }

    enum ModelFamily {
        PULSE("pulse", TagTemplates::pulseItem),
        ULTRA("ultra", TagTemplates::ultraItem),
        PRO("pro", TagTemplates::proItem),
        FAST("fast", TagTemplates::fastItem),
        MINI("mini", TagTemplates::fastItem);

        private final String marker;
        private final Function<TagTemplates, String> format;

        ModelFamily(String marker, Function<TagTemplates, String> format) {
            this.marker = marker;
            this.format = format;
        }

        static Optional<ModelFamily> of(String model) {
            for (ModelFamily family : values()) {
                if (model.contains(family.marker)) {
                    return Optional.of(family);
                }
            }
            return Optional.empty();
        }

        String format(NametagSettings settings) {
            return format.apply(settings.templates());
        }
    }
}
