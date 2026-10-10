// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ReplayScope {
    private final TraceEvent event;
    private final Map<String, ArrayDeque<TraceRecord>> unconsumed = new HashMap<>();
    private final Map<String, TraceRecord> consumed = new HashMap<>();

    ReplayScope(TraceEvent event) {
        this.event = event;
        for (TraceRecord read : event.ownReads()) {
            unconsumed.computeIfAbsent(ReplayReads.keyOf(read), ignored -> new ArrayDeque<>()).addLast(read);
        }
    }

    TraceEvent event() {
        return event;
    }

    TraceRecord next(String key) {
        ArrayDeque<TraceRecord> queue = unconsumed.get(key);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        TraceRecord read = queue.pollFirst();
        consumed.put(key, read);
        return read;
    }

    TraceRecord lastConsumed(String key) {
        return consumed.get(key);
    }

    List<TraceRecord> reads() {
        return event.ownReads();
    }
}
