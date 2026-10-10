// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.util.UUID;

public interface PlayerStanding {
    boolean valid(UUID player);

    boolean exempt(UUID player);

    boolean bedrock(UUID player);

    boolean zoneExcluded(UUID player);

    boolean animating(UUID player);
}
