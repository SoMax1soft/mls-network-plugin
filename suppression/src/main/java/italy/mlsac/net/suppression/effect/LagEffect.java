// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.effect;

import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import java.util.Set;

public record LagEffect(LagLevel level, Set<LagKind> kinds, long start, long end) implements TimedEffect {

    public LagEffect {
        kinds = Set.copyOf(kinds);
    }

    @Override
    public void shelveInto(EffectBook book) {
        book.shelveLag(this);
    }
}
