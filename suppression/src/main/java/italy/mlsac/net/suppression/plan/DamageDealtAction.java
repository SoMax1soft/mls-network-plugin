// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import italy.mlsac.net.suppression.effect.DamageDealtEffect;
import italy.mlsac.net.suppression.effect.TimedEffect;

public record DamageDealtAction(int stage, int percent, int seconds) implements TimedAction {

    @Override
    public TimedEffect effectFrom(long playerTime) {
        return new DamageDealtEffect(percent, playerTime, playerTime + lastsMillis());
    }

    @Override
    public String token() {
        return "-" + percent + "% dmg " + seconds + "s";
    }
}
