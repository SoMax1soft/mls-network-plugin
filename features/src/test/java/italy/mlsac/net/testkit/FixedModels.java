// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.scoring.ModelSwitchboard;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FixedModels implements ModelSwitchboard {

    public boolean ultra;
    public final Set<String> disabled = new HashSet<>();
    public final Set<String> onlyAlert = new HashSet<>();
    public final Map<String, String> names = new HashMap<>();

    public static FixedModels ultraOn() {
        FixedModels models = new FixedModels();
        models.ultra = true;
        return models;
    }

    public static FixedModels ultraOff() {
        return new FixedModels();
    }

    @Override
    public boolean ultraMode() {
        return ultra;
    }

    @Override
    public boolean enabled(String model) {
        return !disabled.contains(model);
    }

    @Override
    public boolean onlyAlertConfigured(String model) {
        return onlyAlert.contains(model);
    }

    @Override
    public String displayName(String model) {
        return names.getOrDefault(model, model);
    }
}
