// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.lag.GroundStrength;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.runtime.BodyCondition;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.lag.runtime.TickLagEffect;
import italy.mlsac.net.suppression.lag.runtime.TickScene;
import italy.mlsac.net.suppression.motion.WorldPoint;
import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

public final class GroundLag implements TickLagEffect {

    private static final Set<BodyCondition> SKIPPING = EnumSet.of(BodyCondition.GLIDING, BodyCondition.SWIMMING,
            BodyCondition.IN_LIQUID, BodyCondition.CLIMBING, BodyCondition.FLYING, BodyCondition.IN_VEHICLE,
            BodyCondition.SLEEPING, BodyCondition.DEAD, BodyCondition.LEVITATING, BodyCondition.SLOW_FALLING,
            BodyCondition.RIPTIDE, BodyCondition.CREATIVE_OR_SPECTATOR);

    private final LagTable table;
    private final GroundPulseRule rule;

    public GroundLag(LagTable table, GroundPulseRule rule) {
        this.table = table;
        this.rule = rule;
    }

    @Override
    public LagKind kind() {
        return LagKind.GROUND;
    }

    @Override
    public void advanceTick(TickScene scene) {
        TickFrame frame = scene.frame();
        GroundState state = scene.runtime().ground();
        Optional<WorldPoint> previous = state.previous().filter(position -> position.sameWorld(frame.position()));
        if (previous.isEmpty() || skipped(scene)) {
            state.resetAirborne();
            state.remember(frame.position(), frame.grounded());
            return;
        }
        double rise = frame.position().y() - previous.get().y();
        boolean wasOnGround = state.previousOnGround();
        int airborne = state.advanceAirborne(frame.grounded());
        GroundStrength strength = table.ground(scene.level());
        OptionalDouble pulse = rule.pulseFor(new GroundStep(wasOnGround, frame.grounded(), rise, airborne), strength);
        if (pulse.isPresent()) {
            scene.actuator().pushVelocity(rule.velocity(pulse.getAsDouble()));
        }
        state.remember(frame.position(), frame.grounded());
    }

    @Override
    public void onInactive(LagRuntime<?> runtime) {
        runtime.ground().reset();
    }

    private static boolean skipped(TickScene scene) {
        return scene.movementPaused() || scene.frame().holdsAnyOf(SKIPPING)
                || scene.runtime().pauses().groundBlocked(scene.frame().tick());
    }
}
