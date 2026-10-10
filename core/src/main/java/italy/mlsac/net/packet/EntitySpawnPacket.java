// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.Optional;
import java.util.UUID;

public record EntitySpawnPacket(UUID viewer, int entityNumber, Optional<UUID> entityUuid, SpawnKind kind,
                                boolean playerEntity, double x, double y, double z) implements OutboundPacket {
}
