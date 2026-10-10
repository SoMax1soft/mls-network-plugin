// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import italy.mlsac.net.suppression.effect.LagEffect;
import italy.mlsac.net.suppression.effect.TimedEffect;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public record LagAction(int stage, LagLevel level, List<LagKind> kinds, int seconds) implements TimedAction {

    public LagAction {
        kinds = List.copyOf(kinds.isEmpty() ? EnumSet.of(LagKind.DELAY, LagKind.MISS) : EnumSet.copyOf(kinds));
    }

    @Override
    public TimedEffect effectFrom(long playerTime) {
        return new LagEffect(level, EnumSet.copyOf(kinds), playerTime, playerTime + lastsMillis());
    }

    @Override
    public String token() {
        String effects = kinds.stream().map(LagKind::id).collect(Collectors.joining(","));
        return "lag " + level.id() + " [" + effects + "] " + seconds + "s";
    }
}
