// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.hit;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.lag.LagLevelSource;
import italy.mlsac.net.suppression.lag.LagTable;
import italy.mlsac.net.suppression.lag.runtime.LagRuntime;
import italy.mlsac.net.suppression.lag.runtime.LagRuntimes;
import java.util.Optional;
import java.util.UUID;

public final class MissLag {

    private final LagLevelSource levels;
    private final LagRuntimes<?> runtimes;
    private final LagTable table;
    private final EngineRandom random;

    public MissLag(LagLevelSource levels, LagRuntimes<?> runtimes, LagTable table, EngineRandom random) {
        this.levels = levels;
        this.runtimes = runtimes;
        this.table = table;
        this.random = random;
    }

    public HitVerdict judge(UUID attackerId, MeleeKind kind, long tick) {
        if (kind == MeleeKind.SWEEP) {
            return runtimes.existing(attackerId).map(runtime -> runtime.missGate().judgeSweep(tick))
                    .orElse(HitVerdict.COUNTS);
        }
        Optional<LagLevel> level = levels.lagLevels(attackerId).levelOf(LagKind.MISS);
        if (level.isEmpty()) {
            runtimes.existing(attackerId).ifPresent(runtime -> runtime.missGate().mainHitCounted(tick));
            return HitVerdict.COUNTS;
        }
        LagRuntime<?> runtime = runtimes.runtimeOf(attackerId);
        return runtime.missGate().judgeMainHit(table.miss(level.get()), tick, random);
    }
}
