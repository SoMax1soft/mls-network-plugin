// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import java.util.Objects;
import java.util.UUID;

public record UltraFlagObserved(UUID player, String playerNick) {

    public UltraFlagObserved {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(playerNick, "playerNick");
    }
}
