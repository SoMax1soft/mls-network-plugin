// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.Objects;
import java.util.UUID;

public record PlayerActionPacket(UUID player, PlayerActionKind kind) implements InboundPacket {

    public PlayerActionPacket {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(kind, "kind");
    }
}
