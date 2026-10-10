// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

final class MarginVerdict {
    private static final double VISIBLE_MARGIN = 0.1;

    private MarginVerdict() {
    }

    static int of(double margin) {
        if (margin < 0) {
            return -1;
        }
        return margin <= VISIBLE_MARGIN ? 0 : 1;
    }
}
