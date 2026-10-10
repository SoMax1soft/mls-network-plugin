// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.ArrayDeque;
import java.util.List;

public final class KickHistory {

    static final int CAPACITY = 10;

    private final ArrayDeque<KickLogLine> records = new ArrayDeque<>(CAPACITY);

    public synchronized void add(KickLogLine record) {
        if (records.size() == CAPACITY) {
            records.removeLast();
        }
        records.addFirst(record);
    }

    public synchronized List<KickLogLine> newestFirst() {
        return List.copyOf(records);
    }

    public synchronized void clear() {
        records.clear();
    }
}
