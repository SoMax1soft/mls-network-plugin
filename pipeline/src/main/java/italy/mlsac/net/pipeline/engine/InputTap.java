// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;

public interface InputTap {

    void movement(MovementPacket packet);

    void noMoveTick(long nanos);

    void teleportConfirmed();

    void swing(long nanos);

    void slotChanged();

    void attack(AttackPacket packet);

    void gameTick();

    void discontinuity();

    void sampleDiscontinuity();
}
