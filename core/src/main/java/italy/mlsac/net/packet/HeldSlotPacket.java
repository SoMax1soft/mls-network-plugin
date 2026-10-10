// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record HeldSlotPacket(UUID player, int slot) implements InboundPacket {
}
