// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.UUID;

public record FlagContext(UUID player, String playerNick, int offenceLevel, double probability, double buffer) {
}
