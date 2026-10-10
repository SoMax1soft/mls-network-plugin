// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

public final class FightMemory {
    private static final int LONG_SPAN = 600;
    private static final int SHORT_SPAN = 200;
    private static final long VALID_AFTER = 100;

    private final int[] airSwings = new int[LONG_SPAN];
    private final int[] attacks = new int[LONG_SPAN];
    private int nextSlot;
    private int held;
    private long appended;
    private long longAir;
    private long longHits;
    private long shortAir;
    private long shortHits;

    public MemorySnapshot record(int airSwingCount, int attackCount) {
        int slot = nextSlot;
        if (held == LONG_SPAN) {
            longAir -= airSwings[slot];
            longHits -= attacks[slot];
        } else {
            held++;
        }
        airSwings[slot] = airSwingCount;
        attacks[slot] = attackCount;
        nextSlot = (slot + 1) % LONG_SPAN;
        longAir += airSwingCount;
        longHits += attackCount;
        shortAir += airSwingCount;
        shortHits += attackCount;
        if (held > SHORT_SPAN) {
            int leaving = (slot - SHORT_SPAN + LONG_SPAN) % LONG_SPAN;
            shortAir -= airSwings[leaving];
            shortHits -= attacks[leaving];
        }
        appended++;
        long total = longAir + longHits;
        double share = total > 0 ? (double) longAir / total : 0.0;
        return new MemorySnapshot(shortAir, shortHits, longAir, longHits, share, appended >= VALID_AFTER);
    }
}
