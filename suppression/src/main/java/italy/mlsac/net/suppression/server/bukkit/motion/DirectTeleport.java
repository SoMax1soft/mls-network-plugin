// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.suppression.server.bukkit.cadence.OwnTeleports;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public final class DirectTeleport implements TeleportRoute {

    private final OwnTeleports ownTeleports;

    public DirectTeleport(OwnTeleports ownTeleports) {
        this.ownTeleports = ownTeleports;
    }

    @Override
    public void travel(Player player, Location target, Runnable arrived) {
        UUID suspectId = player.getUniqueId();
        ownTeleports.begin(suspectId);
        try {
            if (player.teleport(target, TeleportCause.PLUGIN)) {
                arrived.run();
            }
        } finally {
            ownTeleports.finish(suspectId);
        }
    }
}
