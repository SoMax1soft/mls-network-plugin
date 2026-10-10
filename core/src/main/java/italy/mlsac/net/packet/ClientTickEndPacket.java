// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record ClientTickEndPacket(UUID player, long receivedNanos) implements InboundPacket {
}
