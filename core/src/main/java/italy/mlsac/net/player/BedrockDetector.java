// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.UUID;

@FunctionalInterface
public interface BedrockDetector {

    BedrockDetector NONE = player -> false;

    BedrockDetector FLOODGATE_UUID = BedrockDetector::floodgateUuid;

    boolean bedrockClient(UUID player);

    static boolean floodgateUuid(UUID player) {
        return player != null && player.getMostSignificantBits() == 0L;
    }
}
