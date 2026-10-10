// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.UUID;

public record PunishEvent(UUID player, String playerNick, double probability, double buffer, int offenceLevel,
                          String action, String command) {

    public static final String MODEL = "unknown";
}
