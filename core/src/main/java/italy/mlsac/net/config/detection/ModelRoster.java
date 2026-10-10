// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.detection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record ModelRoster(Map<String, ModelSwitch> models) {

    public static final String ULTRA = "ultra";
    public static final List<String> READ_KEYS = List.of("fast", "pro", "ultra", "experimental", "pulse");
    private static final Set<String> NEVER_ONLY_ALERT = Set.of("fast", "pro");

    public ModelRoster {
        models = Collections.unmodifiableMap(new LinkedHashMap<>(models));
    }

    public static ModelRoster pluginDefaults() {
        Map<String, ModelSwitch> defaults = new LinkedHashMap<>();
        defaults.put("fast", ModelSwitch.named("fast", "Fast", true, false));
        defaults.put("pro", ModelSwitch.named("pro", "Pro", true, false));
        defaults.put(ULTRA, ModelSwitch.named(ULTRA, "Ultra", false, false));
        defaults.put("experimental", ModelSwitch.named("experimental", "Experimental", true, true));
        return new ModelRoster(defaults);
    }

    public static boolean onlyAlertAllowed(String key) {
        return !NEVER_ONLY_ALERT.contains(key);
    }

    public boolean ultraMode() {
        ModelSwitch ultra = models.get(ULTRA);
        return ultra != null && ultra.enabled();
    }

    public ModelSwitch lookup(String modelId) {
        ModelSwitch exact = models.get(modelId);
        if (exact != null) {
            return exact;
        }
        String normalised = modelId == null ? "" : modelId.trim().toLowerCase(Locale.ROOT);
        ModelSwitch relaxed = models.get(normalised);
        return relaxed != null ? relaxed : ModelSwitch.unlisted(modelId == null ? "" : modelId);
    }

    public boolean enabled(String modelId) {
        return lookup(modelId).enabled();
    }

    public boolean onlyAlert(String modelId) {
        ModelSwitch model = lookup(modelId);
        return onlyAlertAllowed(model.key()) && model.onlyAlert();
    }

    public String displayName(String modelId) {
        return lookup(modelId).displayNameOrKey();
    }
}
