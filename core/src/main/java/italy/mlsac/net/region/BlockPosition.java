// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.region;

import java.util.UUID;

public record BlockPosition(UUID worldUuid, String worldLabel, int x, int y, int z) {
}
