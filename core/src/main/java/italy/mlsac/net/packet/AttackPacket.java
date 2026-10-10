// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record AttackPacket(UUID player, long receivedNanos, int entityNumber, boolean cancelledEarlier)
        implements InboundPacket {
}
