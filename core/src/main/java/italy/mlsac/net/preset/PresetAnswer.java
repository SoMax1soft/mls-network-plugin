// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public sealed interface PresetAnswer permits PresetAnswer.Delivered, PresetAnswer.Unavailable, PresetAnswer.Failed {

    record Delivered(Optional<String> name, String hash, Map<String, Object> config) implements PresetAnswer {

        public Delivered {
            config = Collections.unmodifiableMap(new LinkedHashMap<>(config));
        }
    }

    record Unavailable(MissingReason reason) implements PresetAnswer {
    }

    record Failed(String cause, boolean authRejected) implements PresetAnswer {
    }
}
