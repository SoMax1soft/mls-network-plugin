// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

final class PaperChargePeriod implements ChargePeriod {

    @Override
    public Float ticks(Player player) {
        return player.getCooldownPeriod();
    }
}
