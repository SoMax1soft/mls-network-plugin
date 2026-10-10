// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.runtime;

import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagLevelSource;
import italy.mlsac.net.suppression.lag.LagLevels;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LagTicker {

    private final LagLevelSource levels;
    private final LagRuntimes<?> runtimes;
    private final CombatLedger combat;
    private final List<TickLagEffect> effects;

    public LagTicker(LagLevelSource levels, LagRuntimes<?> runtimes, CombatLedger combat,
                     List<TickLagEffect> effects) {
        this.levels = levels;
        this.runtimes = runtimes;
        this.combat = combat;
        this.effects = List.copyOf(effects);
    }

    public boolean needsTickWork(UUID suspectId) {
        return levels.lagLevels(suspectId).needsTickWork();
    }

    public void tick(UUID suspectId, TickFrame frame, LagActuator actuator) {
        LagLevels current = levels.lagLevels(suspectId);
        if (!current.needsTickWork()) {
            runtimes.existing(suspectId).ifPresent(runtime -> effects.forEach(effect -> effect.onInactive(runtime)));
            return;
        }
        LagRuntime<?> runtime = runtimes.runtimeOf(suspectId);
        runtime.pauses().settle(frame.tick());
        CombatStatus status = combat.engagedInFight(suspectId, frame.now()) ? CombatStatus.IN_COMBAT : CombatStatus.CALM;
        for (TickLagEffect effect : effects) {
            Optional<LagLevel> level = current.levelOf(effect.kind());
            if (level.isPresent()) {
                effect.advanceTick(new TickScene(frame, level.get(), runtime, status, actuator));
            } else {
                effect.onInactive(runtime);
            }
        }
    }
}
