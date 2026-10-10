// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.rows.TickColumns;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.ArrayDeque;
import java.util.Arrays;

final class FightRun {
    private static final int WINDOW_ROWS = 60;
    private static final int WINDOW_STEP = 30;
    private static final int KEPT_ROWS = WINDOW_ROWS + 1;

    private final WindowListener listener;
    private final FightMemory memory = new FightMemory();
    private final AimAssistTrace assistTrace = new AimAssistTrace();
    private final StrikeTrace strikeTrace = new StrikeTrace();
    private final ArrayDeque<StretchEntry> kept = new ArrayDeque<>();
    private final StretchEntry[] keptScratch = new StretchEntry[KEPT_ROWS + 1];
    private final double[] featureScratch = new double[UltraFeatures.NUMERIC_COLUMNS];
    private final double[] flagScratch = new double[UltraFeatures.BINARY_COLUMNS];
    private long rowsInRun;
    private boolean open;

    FightRun(WindowListener listener) {
        this.listener = listener;
    }

    boolean open() {
        return open;
    }

    void openWith(Iterable<HandedRow> leadIn) {
        open = true;
        leadIn.forEach(this::take);
    }

    void close() {
        open = false;
        assistTrace.forgetShortTerm();
        strikeTrace.forgetShortTerm();
        kept.clear();
        rowsInRun = 0;
    }

    void take(HandedRow handed) {
        TickRow row = handed.row();
        if (row.packet().noPacket()) {
            return;
        }
        MemorySnapshot remembered = memory.record(row.inputs().airSwings(), row.inputs().attacks());
        TickColumns columns = new TickColumns(row);
        AssistReadout assist = assistTrace.observe(AssistReading.from(columns));
        StrikeReadout strike = strikeTrace.observe(StrikeReading.from(columns));
        kept.addLast(new StretchEntry(row, handed.clicks(), remembered, assist, strike));
        if (kept.size() > KEPT_ROWS) {
            kept.pollFirst();
        }
        rowsInRun++;
        if (windowDue() && playerHitInLastStep()) {
            listener.windowReady(window());
        }
    }

    private boolean windowDue() {
        long beyondFirst = rowsInRun - WINDOW_ROWS;
        return beyondFirst >= 0 && beyondFirst % WINDOW_STEP == 0;
    }

    private boolean playerHitInLastStep() {
        long older = Math.max(0, kept.size() - WINDOW_STEP);
        return kept.stream().skip(older).anyMatch(entry -> entry.row().inputs().hitsOnPlayers() > 0);
    }

    private WindowMatrix window() {
        StretchEntry[] entries = kept.toArray(keptScratch);
        int offset = kept.size() - WINDOW_ROWS;
        float[][] features = new float[WINDOW_ROWS][UltraFeatures.NUMERIC_COLUMNS];
        float[][] flags = new float[WINDOW_ROWS][UltraFeatures.BINARY_COLUMNS];
        for (int index = 0; index < WINDOW_ROWS; index++) {
            int position = offset + index;
            StretchEntry before = position > 0 ? entries[position - 1] : null;
            UltraFeatures.fill(entries[position], before, featureScratch, flagScratch, features[index],
                    flags[index]);
        }
        Arrays.fill(keptScratch, null);
        return new WindowMatrix(features, flags);
    }
}
