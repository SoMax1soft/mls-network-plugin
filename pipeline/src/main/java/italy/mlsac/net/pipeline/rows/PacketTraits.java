// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

public record PacketTraits(
        boolean rotationPresent,
        boolean positionPresent,
        boolean grounded,
        boolean cancelled,
        boolean echo,
        boolean noPacket,
        float yaw,
        float pitch) {
}
