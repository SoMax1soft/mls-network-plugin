// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.player.PlayerStanding;
import italy.mlsac.net.player.StandingCache;
import italy.mlsac.net.suppression.server.ExemptionProbe;
import java.util.UUID;

public final class StandingExemption implements ExemptionProbe {

    private final StandingCache standings;

    public StandingExemption(StandingCache standings) {
        this.standings = standings;
    }

    @Override
    public boolean exempt(UUID suspectId) {
        PlayerStanding standing = standings.standing(suspectId);
        return standing.suppressionBypass() || standing.bypass() || standing.bedrock();
    }
}
