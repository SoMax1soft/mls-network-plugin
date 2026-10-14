// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public sealed interface OutboundPacket permits TransactionPingPacket, EntitySpawnPacket, EntityMovePacket,
        EntityDestroyPacket, ViewResetPacket, EntityMetadataPacket, PassengersPacket, PlayerPositionPacket {

    UUID viewer();
}
