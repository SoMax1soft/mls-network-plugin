// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import java.util.UUID;

public record PunishReport(UUID player, String playerNick, double probability, double buffer, int offenceLevel,
                           String action, String command) {
}
