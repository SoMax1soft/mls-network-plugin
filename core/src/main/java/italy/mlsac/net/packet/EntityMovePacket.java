// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record EntityMovePacket(UUID viewer, int entityNumber, MoveKind kind, double x, double y, double z,
                               boolean grounded) implements OutboundPacket {
}
