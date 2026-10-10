// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.UUID;

@FunctionalInterface
public interface BedrockDetector {

    BedrockDetector NONE = player -> false;

    boolean bedrockClient(UUID player);
}
