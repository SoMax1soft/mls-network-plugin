// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

public record RestoreLimits(boolean enabled, int maxPlayers, long maxBytes) {

    public static final RestoreLimits OFF = new RestoreLimits(false, 0, 0);
}
