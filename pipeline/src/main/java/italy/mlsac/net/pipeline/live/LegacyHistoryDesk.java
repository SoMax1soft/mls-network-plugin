// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.detection.LegacyHistoryExport;
import italy.mlsac.net.detection.RestoreResult;
import italy.mlsac.net.pipeline.engine.HistoryExport;
import java.io.UncheckedIOException;
import java.util.UUID;

public final class LegacyHistoryDesk implements LegacyHistoryExport {

    private final HistoryExport pipeline;

    public LegacyHistoryDesk(HistoryExport pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public RestoreResult export(UUID player) {
        try {
            return pipeline.restoreHistory(player) ? RestoreResult.SAVED : RestoreResult.NOTHING_STORED;
        } catch (UncheckedIOException failure) {
            return RestoreResult.FAILED;
        }
    }
}
