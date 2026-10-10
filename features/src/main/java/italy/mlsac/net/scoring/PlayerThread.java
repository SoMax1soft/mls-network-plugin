// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.UUID;

public interface PlayerThread {

    boolean online(UUID player);

    boolean runNextTick(UUID player, Runnable task);
}
