// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import java.util.UUID;

public interface PlayerLifeEvents {
    void joined(UUID player);

    void quit(UUID player);

    void died(UUID player);

    void respawned(UUID player);

    void worldSwitched(UUID player);

    void teleported(UUID player);
}
