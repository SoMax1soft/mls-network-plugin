// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import java.util.UUID;

public interface ClientPacketIntake {
    void movement(MovementPacket packet);

    void clientTickEnd(TickEndPacket packet);

    void teleportConfirmed(UUID player);

    void swing(SwingPacket packet);

    void slotChanged(UUID player);

    void attack(AttackPacket packet);

    boolean pong(UUID player, int transactionId);
}
