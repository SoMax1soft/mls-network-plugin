// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

final class Blinded implements PlayerTrait {

    @Override
    public boolean of(Player player) {
        return player.hasPotionEffect(PotionEffectType.BLINDNESS);
    }
}
