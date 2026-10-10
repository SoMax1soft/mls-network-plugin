// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

public record PlayerStanding(boolean bypass, boolean suppressionBypass, boolean staff, boolean adminOrOp,
                             boolean bedrock) {

    public static final PlayerStanding UNKNOWN = new PlayerStanding(false, false, false, false, false);
}
