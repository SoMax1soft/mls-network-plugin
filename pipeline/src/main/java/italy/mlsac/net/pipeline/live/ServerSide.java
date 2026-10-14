// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.integrity.ClientTickPace;

public record ServerSide(PlayerStates states, WorldSightLines rays, ServerFacts facts, ServerHooks hooks,
                         ClientTickPace pace) {

    public ServerSide(PlayerStates states, WorldSightLines rays, ServerFacts facts) {
        this(states, rays, facts, ServerHooks.NONE);
    }

    public ServerSide(PlayerStates states, WorldSightLines rays, ServerFacts facts, ServerHooks hooks) {
        this(states, rays, facts, hooks, ClientTickPace.VANILLA);
    }
}
