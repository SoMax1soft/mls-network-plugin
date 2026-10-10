// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

public record WindowSubmitted(UUID player, String playerNick, WindowKind kind, long ticket) {
}
