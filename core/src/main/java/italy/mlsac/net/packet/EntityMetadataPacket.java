// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.Optional;
import java.util.UUID;

public record EntityMetadataPacket(UUID viewer, int entityNumber, Optional<PoseKind> pose) implements OutboundPacket {
}
