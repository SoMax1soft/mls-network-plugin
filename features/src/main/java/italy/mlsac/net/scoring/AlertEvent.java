// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.UUID;

public record AlertEvent(UUID player, String playerNick, String model, double probability, double buffer) {
}
