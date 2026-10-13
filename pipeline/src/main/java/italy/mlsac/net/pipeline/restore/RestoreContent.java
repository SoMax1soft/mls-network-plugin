// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import java.time.Instant;
import java.util.UUID;

record RestoreContent(UUID player, UUID sessionKey, Instant beganAt, RowRing ticks, RowRing hits) {

    boolean empty() {
        return ticks.size() == 0 && hits.size() == 0;
    }

    long firstKeptTickStamp() {
        return ticks.dropped() > 0 && ticks.size() > 0 ? ticks.stampAt(0) : Long.MIN_VALUE;
    }
}
