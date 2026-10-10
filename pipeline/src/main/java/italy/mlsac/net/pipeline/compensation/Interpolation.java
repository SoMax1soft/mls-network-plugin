// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;

public interface Interpolation {
    void advance();

    void advanceWithLow();

    Box possible();

    void widenStart(Box extra);
}
