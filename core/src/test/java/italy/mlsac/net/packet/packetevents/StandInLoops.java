// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

final class StandInLoops implements ChannelAccess {

    static final class Loop {
        private final String name;
        private final List<Runnable> queued = new ArrayList<>();

        Loop(String name) {
            this.name = name;
        }

        int queued() {
            return queued.size();
        }

        void runAll() {
            List<Runnable> due = new ArrayList<>(queued);
            queued.clear();
            due.forEach(Runnable::run);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Map<Object, Loop> loops = new IdentityHashMap<>();
    private final List<Object> withoutLoop = new ArrayList<>();

    Object channelOn(Loop loop, String name) {
        Object channel = new StringBuilder(name).toString();
        loops.put(channel, loop);
        return channel;
    }

    Object channelWithBrokenLoop(String name) {
        Object channel = new StringBuilder(name).toString();
        withoutLoop.add(channel);
        return channel;
    }

    @Override
    public Object channelOf(UUID player) {
        return null;
    }

    @Override
    public boolean open(Object channel) {
        return true;
    }

    @Override
    public boolean injected(Object channel) {
        return true;
    }

    @Override
    public void execute(Object channel, Runnable task) {
        Loop loop = loops.get(channel);
        if (loop == null) {
            throw new RejectedExecutionException("no loop for " + channel);
        }
        loop.queued.add(task);
    }

    @Override
    public Object loopOf(Object channel) {
        if (withoutLoop.contains(channel)) {
            throw new IllegalStateException("channel has no event loop");
        }
        return loops.get(channel);
    }
}
