// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import java.util.UUID;

public interface PlayerConnections {
    String clientProtocol(UUID player);

    String clientLabel(UUID player);

    ViewSeed viewSeed(UUID player);

    UUID playerByEntity(int entityNumber);

    String name(UUID player);
}
