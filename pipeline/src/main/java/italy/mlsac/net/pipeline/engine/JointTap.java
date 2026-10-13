// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;

record JointTap(InputTap first, InputTap second) implements InputTap {

    @Override
    public void movement(MovementPacket packet) {
        first.movement(packet);
        second.movement(packet);
    }

    @Override
    public void noMoveTick(long nanos) {
        first.noMoveTick(nanos);
        second.noMoveTick(nanos);
    }

    @Override
    public void teleportConfirmed() {
        first.teleportConfirmed();
        second.teleportConfirmed();
    }

    @Override
    public void swing(long nanos) {
        first.swing(nanos);
        second.swing(nanos);
    }

    @Override
    public void slotChanged() {
        first.slotChanged();
        second.slotChanged();
    }

    @Override
    public void attack(AttackPacket packet) {
        first.attack(packet);
        second.attack(packet);
    }

    @Override
    public void gameTick() {
        first.gameTick();
        second.gameTick();
    }

    @Override
    public void discontinuity() {
        first.discontinuity();
        second.discontinuity();
    }

    @Override
    public void sampleDiscontinuity() {
        first.sampleDiscontinuity();
        second.sampleDiscontinuity();
    }
}
