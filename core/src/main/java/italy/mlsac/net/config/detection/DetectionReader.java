// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.detection;

import italy.mlsac.net.config.SettingsLayer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class DetectionReader {

    private static final Pattern REGION = Pattern.compile("^[A-Za-z0-9_:-]{1,64}$");
    private static final Pattern WORLD_WILDCARD = Pattern.compile("^[A-Za-z0-9_-]{1,64}:\\*$");
    private static final int REGION_CAP = 200;
    private static final int MAX_NAME = 32;

    public DetectionSettings read(DetectionSettings lower, SettingsLayer layer) {
        boolean enabled = layer.bool("detection.enabled").orElse(lower.enabled());
        RegionSettings regions = readRegions(lower.regions(), layer);
        ModelRoster models = readModels(lower.models(), layer);
        int sequence = layer.intClamped("ai.sequence", 1, 10_000).orElse(lower.legacySequence());
        int step = layer.intClamped("ai.step", 1, 10_000).orElse(lower.legacyStep());
        return new DetectionSettings(enabled, regions, models, sequence, step);
    }

    private RegionSettings readRegions(RegionSettings lower, SettingsLayer layer) {
        RegionSettings result = lower.withEnabled(layer.bool("detection.worldguard.enabled").orElse(lower.enabled()));
        Optional<List<?>> list = layer.list("detection.worldguard.disabled-regions");
        if (list.isEmpty()) {
            return result;
        }
        List<RegionEntry> entries = new ArrayList<>();
        int index = 0;
        for (Object raw : list.get()) {
            if (entries.size() >= REGION_CAP) {
                break;
            }
            String path = "detection.worldguard.disabled-regions[" + index++ + "]";
            if (raw instanceof String text && isRegionText(text.trim())) {
                entries.add(RegionEntry.parse(text));
                layer.accepted();
            } else {
                layer.drop(path, "invalid region entry");
            }
        }
        return result.withEntries(entries);
    }

    private static boolean isRegionText(String text) {
        return REGION.matcher(text).matches() || WORLD_WILDCARD.matcher(text).matches();
    }

    private ModelRoster readModels(ModelRoster lower, SettingsLayer layer) {
        Optional<SettingsLayer> section = layer.section("detection.models");
        if (section.isEmpty()) {
            return lower;
        }
        boolean wholesale = layer.source().replacesWholesale();
        Map<String, ModelSwitch> result = wholesale ? new LinkedHashMap<>() : new LinkedHashMap<>(lower.models());
        for (String key : ModelRoster.READ_KEYS) {
            if (!section.get().has(key)) {
                continue;
            }
            Optional<SettingsLayer> model = section.get().section(key);
            if (model.isEmpty()) {
                continue;
            }
            ModelSwitch base = wholesale ? ModelSwitch.unlisted(key) : result.getOrDefault(key, ModelSwitch.unlisted(key));
            result.put(key, modelSwitchOf(key, base, model.get()));
        }
        return new ModelRoster(result);
    }

    private ModelSwitch modelSwitchOf(String key, ModelSwitch base, SettingsLayer model) {
        ModelSwitch result = base;
        if (model.has("name")) {
            result = result.withName(model.text("name", MAX_NAME));
        }
        if (model.has("enabled")) {
            result = result.withEnabled(lenientBool(model, "enabled", true));
        }
        if (model.has("only-alert")) {
            boolean onlyAlert = lenientBool(model, "only-alert", false);
            result = result.withOnlyAlert(onlyAlert && ModelRoster.onlyAlertAllowed(key));
            if (onlyAlert && !ModelRoster.onlyAlertAllowed(key)) {
                model.drop("only-alert", "forced off for model " + key);
            }
        }
        return result;
    }

    private static boolean lenientBool(SettingsLayer layer, String path, boolean fallback) {
        return layer.bool(path).orElse(fallback);
    }
}
