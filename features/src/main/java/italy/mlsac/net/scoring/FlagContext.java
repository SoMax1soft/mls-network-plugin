// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Objects;
import java.util.UUID;

public record FlagContext(UUID player, String playerNick, int offenceLevel, double probability, double buffer,
                          VerdictOrigin origin) {

    public FlagContext {
        Objects.requireNonNull(origin, "origin");
    }

    public FlagContext(UUID player, String playerNick, int offenceLevel, double probability, double buffer) {
        this(player, playerNick, offenceLevel, probability, buffer, VerdictOrigin.MODEL_ANSWER);
    }
}
