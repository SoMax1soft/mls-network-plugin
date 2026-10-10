// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.effect;

import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagLevels;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record EffectSnapshot(List<DamageDealtEffect> dealt, List<DamageTakenEffect> taken, List<LagEffect> lag,
                             long timeShift) {

    private static final EffectSnapshot EMPTY = new EffectSnapshot(List.of(), List.of(), List.of(), 0L);

    public EffectSnapshot {
        dealt = List.copyOf(dealt);
        taken = List.copyOf(taken);
        lag = List.copyOf(lag);
    }

    public static EffectSnapshot empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return dealt.isEmpty() && taken.isEmpty() && lag.isEmpty();
    }

    public long playerTime(long monotonicNow) {
        return monotonicNow - timeShift;
    }

    public int dealtPercent(long monotonicNow) {
        long time = playerTime(monotonicNow);
        int best = 0;
        for (DamageDealtEffect effect : dealt) {
            if (effect.activeAt(time)) {
                best = Math.max(best, effect.percent());
            }
        }
        return best;
    }

    public int takenPercent(long monotonicNow) {
        long time = playerTime(monotonicNow);
        int best = 0;
        for (DamageTakenEffect effect : taken) {
            if (effect.activeAt(time)) {
                best = Math.max(best, effect.percent());
            }
        }
        return best;
    }

    public LagLevels lagLevels(long monotonicNow) {
        if (lag.isEmpty()) {
            return LagLevels.none();
        }
        long time = playerTime(monotonicNow);
        Map<LagKind, LagLevel> levels = new EnumMap<>(LagKind.class);
        for (LagEffect effect : lag) {
            if (effect.activeAt(time)) {
                for (LagKind kind : effect.kinds()) {
                    levels.merge(kind, effect.level(), LagLevel::stronger);
                }
            }
        }
        return LagLevels.of(levels);
    }

    public boolean anyActive(long monotonicNow) {
        long time = playerTime(monotonicNow);
        return dealt.stream().anyMatch(effect -> effect.activeAt(time))
                || taken.stream().anyMatch(effect -> effect.activeAt(time))
                || lag.stream().anyMatch(effect -> effect.activeAt(time));
    }
}
