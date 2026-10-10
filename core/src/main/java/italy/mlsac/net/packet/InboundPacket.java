// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public sealed interface InboundPacket permits MovementPacket, ClientTickEndPacket, TeleportConfirmPacket,
        SwingPacket, HeldSlotPacket, AttackPacket, PongPacket, PlayerActionPacket {

    UUID player();
}
