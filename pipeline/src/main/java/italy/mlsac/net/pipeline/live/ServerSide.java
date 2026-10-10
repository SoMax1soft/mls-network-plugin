// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.WorldSightLines;

public record ServerSide(PlayerStates states, WorldSightLines rays, ServerFacts facts, ServerHooks hooks) {

    public ServerSide(PlayerStates states, WorldSightLines rays, ServerFacts facts) {
        this(states, rays, facts, ServerHooks.NONE);
    }
}
