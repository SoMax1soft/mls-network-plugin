// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record EntityDestroyPacket(UUID viewer, int[] entityNumbers) implements OutboundPacket, RewritableOutbound {

    public EntityDestroyPacket {
        entityNumbers = entityNumbers.clone();
    }

    @Override
    public int[] entityNumbers() {
        return entityNumbers.clone();
    }
}
