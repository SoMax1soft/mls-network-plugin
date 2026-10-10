// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface ServerRoster {

    Collection<? extends Player> online();

    Optional<Player> player(UUID id);

    World worldOrNull(UUID id);
}
