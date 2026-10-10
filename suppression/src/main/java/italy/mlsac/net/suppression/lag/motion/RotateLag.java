// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.RotateStrength;
import italy.mlsac.net.suppression.lag.runtime.BodyCondition;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.lag.runtime.TickLagEffect;
import italy.mlsac.net.suppression.lag.runtime.TickScene;
import java.util.EnumSet;
import java.util.Set;

public final class RotateLag implements TickLagEffect {

    private static final Set<BodyCondition> BLOCKING = EnumSet.of(BodyCondition.DEAD,
            BodyCondition.CREATIVE_OR_SPECTATOR, BodyCondition.SLEEPING);

    private final LagTable table;
    private final EngineRandom random;
    private final TurnRoll roll;

    public RotateLag(LagTable table, EngineRandom random, TurnRoll roll) {
        this.table = table;
        this.random = random;
        this.roll = roll;
    }

    @Override
    public LagKind kind() {
        return LagKind.ROTATE;
    }

    @Override
    public void advanceTick(TickScene scene) {
        TickFrame frame = scene.frame();
        RotateStrength strength = table.rotate(scene.level());
        RotateState state = scene.runtime().rotate();
        state.schedule().keepFor(frame.now(), strength.interval(), random);
        if (!state.schedule().due(frame.now()) || scene.movementPaused() || !scene.pursues(strength.pursuit())
                || frame.holdsAnyOf(BLOCKING)) {
            return;
        }
        Turn turn = roll.roll(strength, frame.pitch());
        scene.actuator().turnView(state.nextSyncId(), turn.yawDelta(), turn.pitchDelta());
        state.schedule().reschedule(frame.now(), random);
    }

    @Override
    public void onInactive(LagRuntime<?> runtime) {
        runtime.rotate().reset();
    }
}
