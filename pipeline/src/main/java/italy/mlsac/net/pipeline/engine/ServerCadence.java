// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import java.util.UUID;

public interface ServerCadence {
    void enabled();

    void disabled();

    void gameTick(long tick);

    void playerTick(UUID player, long tick);
}
