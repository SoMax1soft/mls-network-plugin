// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.damage;

import italy.mlsac.net.suppression.lag.hit.HitVerdict;
import italy.mlsac.net.suppression.lag.hit.MeleeKind;
import italy.mlsac.net.suppression.lag.hit.MissLag;
import italy.mlsac.net.suppression.standing.EffectReadout;
import java.util.Optional;
import java.util.UUID;

public final class DamageSuppression {

    private final EffectReadout readout;
    private final MissLag miss;
    private final DamageCauses causes;
    private final FinalDamageScaler scaler;

    public DamageSuppression(EffectReadout readout, MissLag miss, DamageCauses causes, FinalDamageScaler scaler) {
        this.readout = readout;
        this.miss = miss;
        this.causes = causes;
        this.scaler = scaler;
    }

    public boolean concerns(UUID attackerId, UUID victimId) {
        return readout.touches(attackerId, victimId);
    }

    public DamageOutcome judgeDamage(DamageEventModel event, String cause, UUID attackerId, UUID victimId, long tick) {
        if (!concerns(attackerId, victimId)) {
            return DamageOutcome.UNCHANGED;
        }
        if (attackerId != null && missed(cause, attackerId, tick)) {
            event.cancel();
            return DamageOutcome.MISSED;
        }
        double finalDamage = event.finalDamage();
        if (finalDamage <= 0.0) {
            return DamageOutcome.UNCHANGED;
        }
        int dealt = attackerId != null && causes.dealtApplies(cause) ? readout.dealtPercent(attackerId) : 0;
        int taken = victimId != null && causes.takenApplies(cause) ? readout.takenPercent(victimId) : 0;
        if (dealt == 0 && taken == 0) {
            return DamageOutcome.UNCHANGED;
        }
        DamageMultiplier multiplier = DamageMultiplier.of(dealt, taken);
        if (multiplier.neutral()) {
            return DamageOutcome.UNCHANGED;
        }
        return scaler.scale(event, finalDamage, multiplier);
    }

    private boolean missed(String cause, UUID attackerId, long tick) {
        Optional<MeleeKind> kind = causes.meleeKind(cause);
        return kind.isPresent() && miss.judge(attackerId, kind.get(), tick) == HitVerdict.MISSED;
    }
}
