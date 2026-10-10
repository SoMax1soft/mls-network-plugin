// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import italy.mlsac.net.suppression.execution.PunishmentCooldown;
import italy.mlsac.net.suppression.lag.LagLevelSource;
import italy.mlsac.net.suppression.lag.runtime.CombatLedger;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import italy.mlsac.net.suppression.standing.SuspectStanding;
import java.util.UUID;

public final class SuspectExits {

    private final LagRuntimes<?> lag;
    private final LagLevelSource levels;
    private final CombatLedger combat;
    private final SuspectStanding standing;
    private final PunishmentCooldown cooldown;

    public SuspectExits(LagRuntimes<?> lag, LagLevelSource levels, CombatLedger combat, SuspectStanding standing,
                        PunishmentCooldown cooldown) {
        this.lag = lag;
        this.levels = levels;
        this.combat = combat;
        this.standing = standing;
        this.cooldown = cooldown;
    }

    void quit(UUID suspectId) {
        lag.quit(suspectId);
        combat.forget(suspectId);
        standing.forget(suspectId);
        cooldown.forget(suspectId);
    }

    void sweepIdle() {
        lag.retireIdle(levels);
    }

    void freeEverySuspect() {
        lag.discardAll();
        combat.clear();
        standing.clear();
        cooldown.clear();
    }
}
