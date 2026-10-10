// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record MovementPacket(UUID player, long receivedNanos, float yaw, float pitch, boolean carriesRotation,
                             double x, double y, double z, boolean carriesPosition, boolean grounded,
                             boolean cancelledEarlier) implements InboundPacket {
}
