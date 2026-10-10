// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.effect;

public sealed interface TimedEffect permits DamageDealtEffect, DamageTakenEffect, LagEffect {

    long start();

    long end();

    void shelveInto(EffectBook book);

    default boolean activeAt(long playerTime) {
        return start() <= playerTime && playerTime < end();
    }
}
