// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.body;

import italy.mlsac.net.suppression.server.SuspectBodies;
import italy.mlsac.net.suppression.server.SuspectBody;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitSuspectBodies implements SuspectBodies {

    private final ItemOwnerLock ownerLock;

    public BukkitSuspectBodies(ItemOwnerLock ownerLock) {
        this.ownerLock = ownerLock;
    }

    @Override
    public Optional<SuspectBody> bodyOf(UUID suspectId) {
        Player player = Bukkit.getPlayer(suspectId);
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(new BukkitSuspectBody(player, ownerLock));
    }
}
