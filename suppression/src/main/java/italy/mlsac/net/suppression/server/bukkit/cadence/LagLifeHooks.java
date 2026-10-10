// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.cadence;

import italy.mlsac.net.player.PlayerChangedWorld;
import italy.mlsac.net.player.PlayerDied;
import italy.mlsac.net.player.PlayerRespawned;
import italy.mlsac.net.player.PlayerTeleported;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;

public final class LagLifeHooks {

    private final LagRuntimes<?> runtimes;
    private final ObservedTicks ticks;
    private final OwnTeleports ownTeleports;

    public LagLifeHooks(LagRuntimes<?> runtimes, ObservedTicks ticks, OwnTeleports ownTeleports) {
        this.runtimes = runtimes;
        this.ticks = ticks;
        this.ownTeleports = ownTeleports;
    }

    public void died(PlayerDied death) {
        runtimes.died(death.player());
    }

    public void respawned(PlayerRespawned respawn) {
        runtimes.respawned(respawn.player(), ticks.observedTickOf(respawn.player()));
    }

    public void worldSwitched(PlayerChangedWorld change) {
        runtimes.worldSwitched(change.player(), ticks.observedTickOf(change.player()));
    }

    public void teleported(PlayerTeleported teleport) {
        long tick = ticks.observedTickOf(teleport.player());
        runtimes.teleported(teleport.player(), tick);
        if (!ownTeleports.underway(teleport.player())) {
            runtimes.teleportedByOthers(teleport.player(), tick);
        }
    }
}
