// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import java.util.UUID;

public record AlertReport(UUID player, String playerNick, String model, double probability, double buffer) {
}
