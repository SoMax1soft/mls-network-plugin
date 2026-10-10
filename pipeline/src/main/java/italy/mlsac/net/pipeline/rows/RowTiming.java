// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

public record RowTiming(
        long row,
        long segmentOrdinal,
        long gameTick,
        long nanosIntoRecording,
        double dtMillis,
        int tickEnds,
        boolean engagedInFight,
        int ticksAfterHit,
        double millisAfterHit) {
}
