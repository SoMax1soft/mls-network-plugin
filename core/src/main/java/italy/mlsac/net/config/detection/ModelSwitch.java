// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.detection;

import java.util.Objects;
import java.util.Optional;

public record ModelSwitch(String key, Optional<String> displayName, boolean enabled, boolean onlyAlert) {

    public ModelSwitch {
        Objects.requireNonNull(key, "key");
        displayName = displayName.filter(name -> !name.isEmpty());
    }

    public static ModelSwitch unlisted(String key) {
        return new ModelSwitch(key, Optional.empty(), true, false);
    }

    public static ModelSwitch named(String key, String name, boolean enabled, boolean onlyAlert) {
        return new ModelSwitch(key, Optional.of(name), enabled, onlyAlert);
    }

    public String displayNameOrKey() {
        return displayName.orElse(key);
    }

    public ModelSwitch withName(Optional<String> name) {
        return new ModelSwitch(key, name, enabled, onlyAlert);
    }

    public ModelSwitch withEnabled(boolean value) {
        return new ModelSwitch(key, displayName, value, onlyAlert);
    }

    public ModelSwitch withOnlyAlert(boolean value) {
        return new ModelSwitch(key, displayName, enabled, value);
    }
}
