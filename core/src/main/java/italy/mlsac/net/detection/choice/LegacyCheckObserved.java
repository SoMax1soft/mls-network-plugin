// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import italy.mlsac.net.api.ModelChoice;
import java.util.Objects;
import java.util.UUID;

public record LegacyCheckObserved(UUID player, String playerNick, ModelChoice model, double probability,
                                  double buffer, boolean flagged) {

    public LegacyCheckObserved {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(playerNick, "playerNick");
        if (model != ModelChoice.PRO && model != ModelChoice.MINI) {
            throw new IllegalArgumentException("Only Pro and Mini checks are observed, not " + model);
        }
    }
}
