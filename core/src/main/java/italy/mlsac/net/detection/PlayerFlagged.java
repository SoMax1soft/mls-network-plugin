// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

public record PlayerFlagged(UUID player, String playerNick, int offenceLevel, double probability, double buffer,
                            boolean virtualPlayer) {
}
