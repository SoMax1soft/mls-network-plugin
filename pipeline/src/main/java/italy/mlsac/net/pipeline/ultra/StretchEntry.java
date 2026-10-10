// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.rows.ClickRow;
import italy.mlsac.net.pipeline.rows.TickRow;
import java.util.List;

public record StretchEntry(TickRow row, List<ClickRow> clicks, MemorySnapshot memory, AssistReadout assist,
        StrikeReadout strike, double bodyBearing) {

    public StretchEntry(TickRow row, List<ClickRow> clicks, MemorySnapshot memory, AssistReadout assist,
            StrikeReadout strike) {
        this(row, clicks, memory, assist, strike, UltraFeatures.desired(row));
    }
}
