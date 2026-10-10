// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface ChargePeriod {

    ChargePeriod UNKNOWN = player -> null;

    Float ticks(Player player);

    static ChargePeriod forRunningServer() {
        return MethodPresence.declares(Player.class, "getCooldownPeriod") ? new PaperChargePeriod() : UNKNOWN;
    }
}
