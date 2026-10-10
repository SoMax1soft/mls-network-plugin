// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface ReachReading {

    ReachReading ABSENT = player -> null;

    Double reach(Player player);
}
