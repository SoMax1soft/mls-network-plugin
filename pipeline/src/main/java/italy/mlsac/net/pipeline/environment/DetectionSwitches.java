// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.util.UUID;

public interface DetectionSwitches {
    boolean detectionEnabled();

    boolean ultraEnabled();

    default boolean ultraEnabledFor(UUID player) {
        return ultraEnabled();
    }

    default boolean legacyEnabledFor(UUID player) {
        return !ultraEnabled();
    }

    default int wallChecksPerSecond() {
        return 20;
    }
}
