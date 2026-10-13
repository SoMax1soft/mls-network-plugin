// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.engine.InputTap;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import java.util.function.Consumer;

final class AdmissionTap implements InputTap {
    private final Consumer<AttackPacket> admit;

    AdmissionTap(Consumer<AttackPacket> admit) {
        this.admit = admit;
    }

    @Override
    public void attack(AttackPacket packet) {
        admit.accept(packet);
    }

    @Override
    public void movement(MovementPacket packet) {
    }

    @Override
    public void noMoveTick(long nanos) {
    }

    @Override
    public void teleportConfirmed() {
    }

    @Override
    public void swing(long nanos) {
    }

    @Override
    public void slotChanged() {
    }

    @Override
    public void gameTick() {
    }

    @Override
    public void discontinuity() {
    }

    @Override
    public void sampleDiscontinuity() {
    }
}
