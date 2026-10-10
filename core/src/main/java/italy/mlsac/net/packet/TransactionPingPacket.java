// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record TransactionPingPacket(UUID viewer, int id) implements OutboundPacket {
}
