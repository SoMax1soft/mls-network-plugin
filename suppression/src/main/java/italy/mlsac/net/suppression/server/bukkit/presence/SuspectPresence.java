// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.presence;

import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.region.RegionExemptions;
import italy.mlsac.net.suppression.server.SuspectStatus;
import java.util.UUID;

public final class SuspectPresence implements SuspectStatus {

    private final PlayerDirectory players;
    private final RegionExemptions regions;
    private final GameModePauses modes;

    public SuspectPresence(PlayerDirectory players, RegionExemptions regions, GameModePauses modes) {
        this.players = players;
        this.regions = regions;
        this.modes = modes;
    }

    @Override
    public boolean online(UUID suspectId) {
        return players.byId(suspectId).map(PlayerView::online).orElse(false);
    }

    @Override
    public boolean effectsPaused(UUID suspectId) {
        return modes.paused(suspectId) || regions.lastKnown(suspectId);
    }
}
