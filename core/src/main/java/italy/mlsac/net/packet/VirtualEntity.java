// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record VirtualEntity(int entityNumber, UUID uuid, VirtualEntityType type, double x, double y, double z) {
}
