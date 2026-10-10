// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule;

import java.util.UUID;

public interface LaneRoutes<R> {

    R main();

    R background();

    R player(UUID player, Runnable whenGone);

    R chunk(UUID world, int blockX, int blockZ);
}
