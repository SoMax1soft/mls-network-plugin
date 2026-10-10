// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.UUID;

public record PassengersPacket(UUID viewer, int mountNumber, int[] passengers) implements OutboundPacket, RewritableOutbound {

    public PassengersPacket {
        passengers = passengers.clone();
    }

    @Override
    public int[] passengers() {
        return passengers.clone();
    }
}
