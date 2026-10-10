// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.Pursuit;

public record TickScene(TickFrame frame, LagLevel level, LagRuntime<?> runtime, CombatStatus combat,
                        LagActuator actuator) {

    public boolean movementPaused() {
        return runtime.pauses().movementPaused(frame.tick());
    }

    public boolean engagedInFight() {
        return combat == CombatStatus.IN_COMBAT;
    }

    public boolean pursues(Pursuit pursuit) {
        return !pursuit.waitsForFight() || engagedInFight();
    }
}
