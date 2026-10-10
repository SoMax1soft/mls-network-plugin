// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.OptionalDouble;
import java.util.UUID;

public interface DetectionStates {

    DetectionStates NONE = new DetectionStates() {
        @Override
        public OptionalDouble buffer(UUID player) {
            return OptionalDouble.empty();
        }

        @Override
        public int offenceLevel(UUID player) {
            return 0;
        }
    };

    OptionalDouble buffer(UUID player);

    int offenceLevel(UUID player);
}
