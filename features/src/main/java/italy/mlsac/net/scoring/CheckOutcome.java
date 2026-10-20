// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.UUID;

public record CheckOutcome(UUID player, String playerNick, ScreenedVerdict verdict, double buffer, boolean flagged) {
}
