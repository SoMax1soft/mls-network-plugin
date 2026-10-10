// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

public record ClickTiming(long row, long segmentOrdinal, long gameTick, long nanosIntoRecording, double resolveMillis,
        int rotationSource) {
}
