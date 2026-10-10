// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.TickRow;
import italy.mlsac.net.pipeline.ultra.FightAssembler;
import italy.mlsac.net.pipeline.ultra.MlsuWire;
import italy.mlsac.net.pipeline.ultra.WireVersion;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecordedRowsWindowTest {
    private static final String PLAYER = "9caa6f4f-a211-3cd2-9a4f-c09f810194a9";
    private static final String FOLDER = "LEGIT_Legit_01_";
    private static final int FIRST_COMPARED = 10;
    private static final int LAST_COMPARED = 37;

    @Test
    void windowsRebuiltFromRecordedRowsMatchTheLiveWindows() {
        List<byte[]> recordedWindows = new ArrayList<>();
        String[] tables = new String[2];
        new TraceFile(GoldenTraces.locate("ultra_legit_duel")).forEachEvent(event -> {
            for (TraceRecord output : event.outputs()) {
                if ("ng.window".equals(output.name()) && PLAYER.equals(output.text("p"))) {
                    recordedWindows.add(Base64.getDecoder().decode(output.text("b64")));
                }
                if ("file".equals(output.name()) && output.text("path").contains(FOLDER)) {
                    if (output.text("path").endsWith("/ticks.csv")) {
                        tables[0] = output.text("content");
                    }
                    if (output.text("path").endsWith("/hits.csv")) {
                        tables[1] = output.text("content");
                    }
                }
            }
        });
        List<byte[]> rebuilt = new ArrayList<>();
        FightAssembler assembler = new FightAssembler(window -> rebuilt.add(
                MlsuWire.encode(WireVersion.V4, window.narrowedTo(WireVersion.V4))));
        List<ClickRow> clicks = clicks(tables[1]);
        String[] tickLines = tables[0].split("\n");
        StoredRows ticks = new StoredRows(tickLines[0]);
        int nextClick = 0;
        for (int line = 1; line < tickLines.length; line++) {
            TickRow row = ticks.line(tickLines[line]).tick();
            while (nextClick < clicks.size() && clicks.get(nextClick).timing().row() <= row.timing().row()) {
                if (clicks.get(nextClick).timing().row() == row.timing().row()) {
                    assembler.clickRow(clicks.get(nextClick));
                }
                nextClick++;
            }
            assembler.tickRow(row);
        }
        int offset = rebuilt.size() - recordedWindows.size();
        for (int index = FIRST_COMPARED; index <= LAST_COMPARED; index++) {
            assertArrayEquals(recordedWindows.get(index - 1), rebuilt.get(index - 1 + offset), "window " + index);
        }
        assertEquals(LAST_COMPARED, recordedWindows.size());
    }

    private static List<ClickRow> clicks(String table) {
        String[] lines = table.split("\n");
        StoredRows rows = new StoredRows(lines[0]);
        List<ClickRow> clicks = new ArrayList<>();
        for (int line = 1; line < lines.length; line++) {
            ClickRow click = rows.line(lines[line]).click();
            if (click.timing().row() >= 0) {
                clicks.add(click);
            }
        }
        clicks.sort((left, right) -> Long.compare(left.timing().row(), right.timing().row()));
        return clicks;
    }
}
