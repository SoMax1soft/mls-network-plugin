// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.RowSink;
import italy.mlsac.net.pipeline.rows.TickRow;
import italy.mlsac.net.pipeline.tables.HitLine;
import italy.mlsac.net.pipeline.tables.TickLine;
import java.util.ArrayDeque;

final class KeptRing implements RowSink {
    static final int LEAD_IN_ROWS = 40;
    static final int WAITING_LIMIT = 512;

    private final ArrayDeque<TickRow> leadIn = new ArrayDeque<>();
    private final ArrayDeque<TickRow> waitingTicks = new ArrayDeque<>();
    private final ArrayDeque<ClickRow> waitingClicks = new ArrayDeque<>();
    private final RowRing ticks;
    private final RowRing hits;
    private final ChunkClaim claim;

    KeptRing(RowRing ticks, RowRing hits, ChunkClaim claim) {
        this.ticks = ticks;
        this.hits = hits;
        this.claim = claim;
    }

    @Override
    public void tickRow(TickRow row) {
        if (row.timing().engagedInFight()) {
            waitingTicks.addLast(row);
            settleWhenCrowded();
            return;
        }
        leadIn.addLast(row);
        if (leadIn.size() > LEAD_IN_ROWS) {
            leadIn.pollFirst();
        }
    }

    @Override
    public void clickRow(ClickRow row) {
        waitingClicks.addLast(row);
        settleWhenCrowded();
    }

    @Override
    public void fightStarted() {
        while (!leadIn.isEmpty()) {
            waitingTicks.addLast(leadIn.pollFirst());
        }
    }

    @Override
    public void segmentBroken() {
        leadIn.clear();
    }

    void settle() {
        while (!waitingTicks.isEmpty()) {
            TickRow row = waitingTicks.pollFirst();
            TickLine.write(row, ticks.stage());
            ticks.commit(row.timing().nanosIntoRecording(), claim);
        }
        while (!waitingClicks.isEmpty()) {
            ClickRow row = waitingClicks.pollFirst();
            HitLine.write(row, hits.stage());
            hits.commit(row.timing().nanosIntoRecording(), claim);
        }
    }

    RowRing ticks() {
        return ticks;
    }

    RowRing hits() {
        return hits;
    }

    int waiting() {
        return leadIn.size() + waitingTicks.size() + waitingClicks.size();
    }

    void release() {
        leadIn.clear();
        waitingTicks.clear();
        waitingClicks.clear();
        ticks.release();
        hits.release();
    }

    private void settleWhenCrowded() {
        if (waitingTicks.size() + waitingClicks.size() > WAITING_LIMIT) {
            settle();
        }
    }
}
