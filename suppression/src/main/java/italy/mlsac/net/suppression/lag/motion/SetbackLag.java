// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.motion;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.SetbackStrength;
import italy.mlsac.net.suppression.lag.runtime.BodyCondition;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.TickFrame;
import italy.mlsac.net.suppression.lag.runtime.TickLagEffect;
import italy.mlsac.net.suppression.lag.runtime.TickScene;
import italy.mlsac.net.suppression.motion.WorldPoint;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class SetbackLag implements TickLagEffect {

    private static final Set<BodyCondition> BLOCKING = EnumSet.of(BodyCondition.GLIDING, BodyCondition.IN_VEHICLE,
            BodyCondition.SLEEPING, BodyCondition.DEAD, BodyCondition.CREATIVE_OR_SPECTATOR);

    private final LagTable table;
    private final EngineRandom random;
    private final SetbackTerrain terrain;

    public SetbackLag(LagTable table, EngineRandom random, SetbackTerrain terrain) {
        this.table = table;
        this.random = random;
        this.terrain = terrain;
    }

    @Override
    public LagKind kind() {
        return LagKind.SETBACK;
    }

    @Override
    public void advanceTick(TickScene scene) {
        TickFrame frame = scene.frame();
        SetbackStrength strength = table.setback(scene.level());
        SetbackState state = scene.runtime().setback();
        state.schedule().keepFor(frame.now(), strength.interval(), random);
        if (scene.movementPaused()) {
            return;
        }
        state.history().append(frame.position());
        if (!state.schedule().due(frame.now()) || !scene.pursues(strength.pursuit())) {
            return;
        }
        target(frame, state, strength).ifPresent(target -> {
            scene.actuator().setBack(target, frame.fallBlocks());
            scene.runtime().pauses().teleported(frame.tick());
            state.history().clear();
        });
        state.schedule().reschedule(frame.now(), random);
    }

    @Override
    public void onInactive(LagRuntime<?> runtime) {
        runtime.setback().reset();
    }

    private Optional<WorldPoint> target(TickFrame frame, SetbackState state, SetbackStrength strength) {
        if (frame.holdsAnyOf(BLOCKING)) {
            return Optional.empty();
        }
        WorldPoint current = frame.position();
        Predicate<WorldPoint> withinReach = candidate -> candidate.sameWorld(current)
                && strength.reach().admits(current.gapTo(candidate));
        Optional<WorldPoint> rewound = state.history().ticksBack(strength.rewindTicks()).filter(withinReach);
        if (rewound.isEmpty() && strength.pursuit().fallsBackWithinReach()) {
            rewound = state.history().newestMatching(withinReach);
        }
        return rewound.filter(terrain::chunkLoaded).filter(terrain::passable);
    }
}
