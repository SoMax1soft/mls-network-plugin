// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

final class NativeWaterContact implements PlayerTrait {

    @Override
    public boolean of(Player player) {
        return player.isInWater();
    }
}
