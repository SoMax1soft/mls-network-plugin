// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.motion.WorldPoint;

public interface SetbackTerrain {

    boolean chunkLoaded(WorldPoint position);

    boolean passable(WorldPoint position);
}
