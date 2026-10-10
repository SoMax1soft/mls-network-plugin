// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class RecentEventIds {

    private final int capacity;
    private final Set<String> known = new HashSet<>();
    private final ArrayDeque<String> arrival = new ArrayDeque<>();

    RecentEventIds(int capacity) {
        this.capacity = capacity;
    }

    synchronized boolean firstSighting(String eventId) {
        String key = Objects.requireNonNullElse(eventId, "");
        if (!known.add(key)) {
            return false;
        }
        arrival.addLast(key);
        if (arrival.size() > capacity) {
            known.remove(arrival.removeFirst());
        }
        return true;
    }
}
