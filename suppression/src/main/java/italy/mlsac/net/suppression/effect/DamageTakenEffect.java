// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.effect;

public record DamageTakenEffect(int percent, long start, long end) implements TimedEffect {

    @Override
    public void shelveInto(EffectBook book) {
        book.shelveTaken(this);
    }
}
