// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

import java.util.UUID;

public record RestoreRequest(UUID player, RestoreLabel label, String staffName) {
}
