// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

final class FlushedProbes {
    private static final int INITIAL_ROOM = 16;

    private int[] ids = new int[INITIAL_ROOM];
    private long[] flushedAt = new long[INITIAL_ROOM];
    private int oldest;
    private int waiting;
    private int flushedTotal;
    private int answeredTotal;

    void flushed(int id, long nanos) {
        if (waiting == ids.length) {
            widen();
        }
        int slot = (oldest + waiting) % ids.length;
        ids[slot] = id;
        flushedAt[slot] = nanos;
        waiting++;
        flushedTotal++;
    }

    int positionOf(int id) {
        for (int step = 0; step < waiting; step++) {
            if (ids[(oldest + step) % ids.length] == id) {
                return step;
            }
        }
        return -1;
    }

    long retireThrough(int position) {
        long sentAt = flushedAt[(oldest + position) % ids.length];
        int retired = position + 1;
        oldest = (oldest + retired) % ids.length;
        waiting -= retired;
        answeredTotal += retired;
        return sentAt;
    }

    int flushedTotal() {
        return flushedTotal;
    }

    int answeredTotal() {
        return answeredTotal;
    }

    private void widen() {
        int[] wideIds = new int[ids.length * 2];
        long[] wideTimes = new long[ids.length * 2];
        for (int step = 0; step < waiting; step++) {
            wideIds[step] = ids[(oldest + step) % ids.length];
            wideTimes[step] = flushedAt[(oldest + step) % ids.length];
        }
        ids = wideIds;
        flushedAt = wideTimes;
        oldest = 0;
    }
}
