// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.Optional;

public record PresetState(boolean applied, Optional<String> resolvedName, String hash) {

    public static PresetState none() {
        return new PresetState(false, Optional.empty(), "");
    }
}
