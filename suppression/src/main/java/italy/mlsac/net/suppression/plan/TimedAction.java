// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import italy.mlsac.net.suppression.effect.TimedEffect;

public sealed interface TimedAction extends InGameAction permits DamageDealtAction, DamageTakenAction, LagAction {

    int seconds();

    TimedEffect effectFrom(long playerTime);

    String token();

    default long lastsMillis() {
        return seconds() * 1000L;
    }

    @Override
    default void offerTo(InGameActionSink sink) {
        sink.timed(this);
    }
}
