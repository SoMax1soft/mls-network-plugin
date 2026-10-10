// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import java.util.UUID;

public interface ServerFacts {

    boolean valid(UUID player);

    ViewSeed viewSeed(UUID player);

    UUID playerByEntity(int entityNumber);
}
