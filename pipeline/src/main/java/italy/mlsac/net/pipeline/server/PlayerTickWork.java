// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.UUID;
import org.bukkit.entity.Player;

public interface PlayerTickWork {

    void perform(Player player, long tick);

    void forget(UUID player);
}
