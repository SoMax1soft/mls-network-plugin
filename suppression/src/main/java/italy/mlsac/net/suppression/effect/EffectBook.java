// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.effect;

public final class EffectBook {

    private final EffectShelf<DamageDealtEffect> dealt = new EffectShelf<>();
    private final EffectShelf<DamageTakenEffect> taken = new EffectShelf<>();
    private final EffectShelf<LagEffect> lag = new EffectShelf<>();

    public void add(TimedEffect effect) {
        effect.shelveInto(this);
    }

    void shelveDealt(DamageDealtEffect effect) {
        dealt.add(effect);
    }

    void shelveTaken(DamageTakenEffect effect) {
        taken.add(effect);
    }

    void shelveLag(LagEffect effect) {
        lag.add(effect);
    }

    public void pruneEndedBy(long playerTime) {
        dealt.pruneEndedBy(playerTime);
        taken.pruneEndedBy(playerTime);
        lag.pruneEndedBy(playerTime);
    }

    public boolean isEmpty() {
        return dealt.isEmpty() && taken.isEmpty() && lag.isEmpty();
    }

    public EffectSnapshot snapshot(long timeShift) {
        if (isEmpty()) {
            return EffectSnapshot.empty();
        }
        return new EffectSnapshot(dealt.copy(), taken.copy(), lag.copy(), timeShift);
    }
}
