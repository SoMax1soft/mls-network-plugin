// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.RowSink;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FightAssembler implements RowSink {
    private static final int DELAY_ROWS = 2;
    private static final int LEAD_IN_ROWS = Integer.getInteger("mlsac.leadInRows", 0);

    private final Map<Long, List<ClickRow>> waitingClicks = new HashMap<>();
    private final ArrayDeque<HandedRow> delayLine = new ArrayDeque<>();
    private final ArrayDeque<HandedRow> leadIn = new ArrayDeque<>();
    private final FightRun run;
    private Long currentSegment;

    public FightAssembler(WindowListener listener) {
        this.run = new FightRun(listener);
    }

    @Override
    public void clickRow(ClickRow row) {
        long rowNumber = row.timing().row();
        if (rowNumber >= 0) {
            waitingClicks.computeIfAbsent(rowNumber, ignored -> new ArrayList<>()).add(row);
        }
    }

    @Override
    public void tickRow(TickRow row) {
        List<ClickRow> clicks = waitingClicks.remove(row.timing().row());
        delayLine.addLast(new HandedRow(row, Objects.requireNonNullElse(clicks, List.of())));
        while (delayLine.size() > DELAY_ROWS) {
            route(delayLine.pollFirst());
        }
    }

    @Override
    public void fightStarted() {
    }

    @Override
    public void segmentBroken() {
    }

    private void route(HandedRow handed) {
        var timing = handed.row().timing();
        Long segment = Long.valueOf(timing.segmentOrdinal());
        if (!segment.equals(currentSegment)) {
            leadIn.clear();
            run.close();
            currentSegment = segment;
        }
        if (timing.engagedInFight()) {
            joinFight(handed);
            return;
        }
        run.close();
        leadIn.addLast(handed);
        if (leadIn.size() > LEAD_IN_ROWS) {
            leadIn.pollFirst();
        }
    }

    private void joinFight(HandedRow handed) {
        if (!run.open()) {
            run.openWith(leadIn);
            leadIn.clear();
        }
        run.take(handed);
    }
}
