// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

final class StandInChannel {

    private final ExecutorService loop;
    private final boolean open;
    private final boolean handlers;
    private final boolean failingCheck;
    private final AtomicInteger scheduled = new AtomicInteger();
    private final AtomicInteger escaped = new AtomicInteger();

    private StandInChannel(ExecutorService loop, boolean open, boolean handlers, boolean failingCheck) {
        this.loop = loop;
        this.open = open;
        this.handlers = handlers;
        this.failingCheck = failingCheck;
    }

    static StandInChannel injected(ExecutorService loop) {
        return new StandInChannel(loop, true, true, false);
    }

    static ChannelAccess absent() {
        return new ChannelAccess() {
            @Override
            public Object channelOf(UUID player) {
                return null;
            }

            @Override
            public boolean open(Object channel) {
                throw new AssertionError("no channel to check");
            }

            @Override
            public boolean injected(Object channel) {
                throw new AssertionError("no channel to check");
            }

            @Override
            public void execute(Object channel, Runnable task) {
                throw new AssertionError("no channel to run on");
            }
        };
    }

    StandInChannel withoutHandlers() {
        return new StandInChannel(loop, open, false, failingCheck);
    }

    StandInChannel closed() {
        return new StandInChannel(loop, false, handlers, failingCheck);
    }

    StandInChannel withFailingCheck() {
        return new StandInChannel(loop, open, handlers, true);
    }

    int scheduled() {
        return scheduled.get();
    }

    int escaped() {
        return escaped.get();
    }

    ChannelAccess access() {
        StandInChannel channel = this;
        return new ChannelAccess() {
            @Override
            public Object channelOf(UUID player) {
                return channel;
            }

            @Override
            public boolean open(Object target) {
                return open;
            }

            @Override
            public boolean injected(Object target) {
                if (failingCheck) {
                    throw new IllegalStateException("pipeline gone");
                }
                return handlers;
            }

            @Override
            public void execute(Object target, Runnable task) {
                scheduled.incrementAndGet();
                loop.execute(() -> {
                    try {
                        task.run();
                    } catch (RuntimeException | Error failure) {
                        escaped.incrementAndGet();
                    }
                });
            }
        };
    }
}
