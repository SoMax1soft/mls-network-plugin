// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import italy.mlsac.net.suppression.plan.TrickType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TrickEffects {

    private final Map<TrickType, TrickEffect> byType;

    public TrickEffects(List<TrickEffect> effects) {
        Map<TrickType, TrickEffect> map = new EnumMap<>(TrickType.class);
        effects.forEach(effect -> map.put(effect.type(), effect));
        this.byType = Collections.unmodifiableMap(map);
    }

    public Optional<TrickEffect> effectFor(TrickType type) {
        return Optional.ofNullable(byType.get(type));
    }
}
