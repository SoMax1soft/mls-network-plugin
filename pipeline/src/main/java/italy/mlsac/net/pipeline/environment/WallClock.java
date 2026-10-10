// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.time.Instant;

public interface WallClock {
    Instant instant(WallPurpose purpose);

    String localStamp(WallPurpose purpose, String pattern);
}
