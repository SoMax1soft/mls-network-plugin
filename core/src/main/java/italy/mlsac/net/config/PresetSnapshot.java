// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.Objects;
import java.util.Optional;

public record PresetSnapshot(Optional<String> name, String hash, ConfigTree effectiveLayout) {

    public PresetSnapshot {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(effectiveLayout, "effectiveLayout");
    }

    public PresetState state() {
        return new PresetState(true, name, hash);
    }
}
