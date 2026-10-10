// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.packet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class HeldPackets<P> {

    public static final int CAPACITY = 40;

    private final Deque<Held<P>> queue = new ArrayDeque<>();

    public synchronized List<P> holdDelayed(P packet, long receivedAt, long drawDelayMillis) {
        return enqueue(packet, Math.max(receivedAt + drawDelayMillis, lastDue()));
    }

    public synchronized List<P> holdBehindQueue(P packet) {
        return enqueue(packet, lastDue());
    }

    public synchronized boolean hasQueued() {
        return !queue.isEmpty();
    }

    public synchronized List<P> releaseDue(long now) {
        List<P> released = new ArrayList<>();
        while (!queue.isEmpty() && queue.peekFirst().due() <= now) {
            released.add(queue.pollFirst().packet());
        }
        return released;
    }

    public synchronized List<P> releaseAll() {
        return releaseDue(Long.MAX_VALUE);
    }

    public synchronized List<Long> dueTimes() {
        List<Long> times = new ArrayList<>();
        queue.forEach(held -> times.add(held.due()));
        return times;
    }

    public synchronized List<P> discard() {
        List<P> dropped = new ArrayList<>(queue.size());
        queue.forEach(held -> dropped.add(held.packet()));
        queue.clear();
        return dropped;
    }

    private List<P> enqueue(P packet, long due) {
        List<P> releasedEarly = List.of();
        if (queue.size() >= CAPACITY) {
            releasedEarly = List.of(queue.pollFirst().packet());
        }
        queue.addLast(new Held<>(packet, due));
        return releasedEarly;
    }

    private long lastDue() {
        Held<P> last = queue.peekLast();
        return last == null ? Long.MIN_VALUE : last.due();
    }

    private record Held<P>(P packet, long due) {
    }
}
