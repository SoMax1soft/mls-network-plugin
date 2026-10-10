// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.schedule.LaneRoutes;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskScheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManualScheduler implements TaskScheduler {

    public enum Lane {
        GLOBAL,
        ASYNC,
        PLAYER,
        REGION
    }

    public final class Entry implements TaskTicket {
        private final Runnable task;
        private final Lane lane;
        private final long period;
        private final long order;
        private final UUID player;
        private long due;
        private volatile boolean cancelled;

        Entry(Runnable task, Lane lane, long due, long period, UUID player) {
            this.task = task;
            this.lane = lane;
            this.due = due;
            this.period = period;
            this.player = player;
            this.order = sequence++;
        }

        public Lane lane() {
            return lane;
        }

        public long due() {
            return due;
        }

        public long period() {
            return period;
        }

        public UUID player() {
            return player;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Set<UUID> retiredPlayers = ConcurrentHashMap.newKeySet();
    private long now;
    private long sequence;

    public synchronized long now() {
        return now;
    }

    public void retire(UUID player) {
        retiredPlayers.add(player);
    }

    @Override
    public TaskTicket submit(TaskLane lane, TaskTiming timing, Runnable task) {
        long delay = timing.cadence() == TaskTiming.Cadence.NOW ? 0 : timing.delayTicks();
        long period = timing.repeats() ? Math.max(1, timing.everyTicks()) : 0;
        return lane.route(new LaneRoutes<TaskTicket>() {
            @Override
            public TaskTicket main() {
                return add(task, Lane.GLOBAL, delay, period, null);
            }

            @Override
            public TaskTicket background() {
                return add(task, Lane.ASYNC, delay, period, null);
            }

            @Override
            public TaskTicket player(UUID player, Runnable whenGone) {
                return add(guard(player, task, whenGone), Lane.PLAYER, delay, period, player);
            }

            @Override
            public TaskTicket chunk(UUID world, int blockX, int blockZ) {
                return add(task, Lane.REGION, delay, period, null);
            }
        });
    }

    @Override
    public boolean onGlobalThread() {
        return true;
    }

    @Override
    public synchronized void revokeEvery() {
        entries.forEach(Entry::cancel);
        entries.clear();
    }

    public void runPending() {
        advance(0);
    }

    public void advance(long ticks) {
        long target;
        synchronized (this) {
            target = now + ticks;
        }
        while (true) {
            Entry next;
            synchronized (this) {
                entries.removeIf(Entry::isCancelled);
                next = entries.stream().filter(e -> e.due <= target)
                        .min(Comparator.comparingLong((Entry e) -> e.due).thenComparingLong(e -> e.order))
                        .orElse(null);
                if (next == null) {
                    now = target;
                    return;
                }
                now = Math.max(now, next.due);
                if (next.period > 0) {
                    next.due = now + next.period;
                } else {
                    entries.remove(next);
                }
            }
            next.task.run();
        }
    }

    public synchronized List<Entry> active() {
        entries.removeIf(Entry::isCancelled);
        return List.copyOf(entries);
    }

    private Runnable guard(UUID player, Runnable task, Runnable onRetired) {
        return () -> {
            if (retiredPlayers.contains(player)) {
                onRetired.run();
            } else {
                task.run();
            }
        };
    }

    private synchronized Entry add(Runnable task, Lane lane, long delay, long period, UUID player) {
        Entry entry = new Entry(task, lane, now + Math.max(0, delay), period, player);
        entries.add(entry);
        return entry;
    }
}
