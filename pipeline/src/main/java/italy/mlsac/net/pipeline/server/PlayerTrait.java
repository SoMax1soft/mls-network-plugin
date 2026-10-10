// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerTrait {

    boolean of(Player player);
}
