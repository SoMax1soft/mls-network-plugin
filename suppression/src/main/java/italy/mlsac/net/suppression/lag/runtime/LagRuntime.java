// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.lag.hit.MissGate;
import italy.mlsac.net.suppression.lag.motion.GroundState;
import italy.mlsac.net.suppression.lag.motion.RotateState;
import italy.mlsac.net.suppression.lag.motion.SetbackState;
import italy.mlsac.net.suppression.lag.packet.HeldPackets;

public final class LagRuntime<P> {

    private final HeldPackets<P> heldPackets = new HeldPackets<>();
    private final MissGate missGate = new MissGate();
    private final LagPauses pauses = new LagPauses();
    private final SetbackState setback = new SetbackState();
    private final GroundState ground = new GroundState();
    private final RotateState rotate = new RotateState();

    public HeldPackets<P> heldPackets() {
        return heldPackets;
    }

    public MissGate missGate() {
        return missGate;
    }

    public LagPauses pauses() {
        return pauses;
    }

    public SetbackState setback() {
        return setback;
    }

    public GroundState ground() {
        return ground;
    }

    public RotateState rotate() {
        return rotate;
    }

    public synchronized void forgetMovement() {
        setback.history().clear();
        ground.reset();
    }
}
