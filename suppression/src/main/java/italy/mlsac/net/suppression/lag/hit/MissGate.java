// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.hit;

import italy.mlsac.net.suppression.chance.EngineRandom;
import italy.mlsac.net.suppression.lag.MissStrength;

public final class MissGate {

    private int consecutiveMisses;
    private long lastMainTick = Long.MIN_VALUE;
    private HitVerdict lastMainVerdict = HitVerdict.COUNTS;

    public synchronized HitVerdict judgeMainHit(MissStrength strength, long tick, EngineRandom random) {
        HitVerdict verdict;
        if (strength.capReachedAfter(consecutiveMisses)) {
            verdict = HitVerdict.COUNTS;
        } else {
            verdict = random.nextUnit() < strength.chance() ? HitVerdict.MISSED : HitVerdict.COUNTS;
        }
        consecutiveMisses = verdict == HitVerdict.MISSED ? oneMore(consecutiveMisses) : 0;
        remember(tick, verdict);
        return verdict;
    }

    public synchronized void mainHitCounted(long tick) {
        remember(tick, HitVerdict.COUNTS);
    }

    public synchronized HitVerdict judgeSweep(long tick) {
        return tick == lastMainTick ? lastMainVerdict : HitVerdict.COUNTS;
    }

    private static int oneMore(int misses) {
        return misses == Integer.MAX_VALUE ? misses : misses + 1;
    }

    private void remember(long tick, HitVerdict verdict) {
        lastMainTick = tick;
        lastMainVerdict = verdict;
    }
}
