// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

public record ModelVerdict(UUID player, String playerNick, String model, double probability, WindowKind kind,
                           long ticket) {
}
