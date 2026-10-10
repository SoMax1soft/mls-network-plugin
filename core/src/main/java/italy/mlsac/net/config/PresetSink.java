// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.Optional;

@FunctionalInterface
public interface PresetSink {

    void apply(Optional<PresetSnapshot> snapshot);
}
