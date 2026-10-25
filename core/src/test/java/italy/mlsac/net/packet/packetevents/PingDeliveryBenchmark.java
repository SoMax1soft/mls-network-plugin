// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PingDeliveryBenchmark {

    private static final int LOOPS = 16;
    private static final int WARMUP_TICKS = 600;
    private static final int MEASURED_TICKS = 600;
    private static final int ROUNDS = 3;
    private static final long IDLE_BETWEEN_TICKS_NANOS = 300_000L;

    @Test
    void serverThreadCostOfOneTickOfPings() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("mlsac.bench"), "benchmark runs with -Pbench=true");
        int viewers = Integer.getInteger("mlsac.bench.players", 126);
        EventLoopGroup group = new DefaultEventLoopGroup(LOOPS);
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "viewers=%d loops=%d ticks=%d%n", viewers, LOOPS, MEASURED_TICKS));
        try {
            for (PingSendMode mode : List.of(PingSendMode.LOOP, PingSendMode.BATCH)) {
                Fixture fixture = new Fixture(group, viewers, mode);
                fixture.play(WARMUP_TICKS);
                long[] rounds = new long[ROUNDS];
                for (int round = 0; round < ROUNDS; round++) {
                    fixture.submits.set(0);
                    rounds[round] = fixture.play(MEASURED_TICKS);
                }
                Arrays.sort(rounds);
                fixture.drain();
                assertEquals((long) (WARMUP_TICKS + ROUNDS * MEASURED_TICKS) * viewers, fixture.written.get());
                out.append(String.format(Locale.ROOT,
                        "%-5s server thread %.4f ms/tick (%.2f us/viewer), loop hand-offs %.1f/tick%n",
                        mode.key(), rounds[ROUNDS / 2] / (double) MEASURED_TICKS / 1e6,
                        rounds[ROUNDS / 2] / (double) MEASURED_TICKS / 1e3 / viewers,
                        fixture.submits.get() / (double) MEASURED_TICKS));
            }
        } finally {
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS).await(5, TimeUnit.SECONDS);
        }
        System.out.println(out);
        Path report = Path.of("build", "bench", "ping-delivery-" + viewers + ".txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, out.toString(), StandardCharsets.UTF_8);
    }

    private static final class Fixture {
        private final List<UUID> viewers = new ArrayList<>();
        private final Map<UUID, Object> channels = new HashMap<>();
        private final AtomicLong submits = new AtomicLong();
        private final AtomicLong written = new AtomicLong();
        private final List<EventLoop> loops = new ArrayList<>();
        private final ReachableChannels reachable;
        private final PingDelivery delivery;
        private int id;

        Fixture(EventLoopGroup group, int count, PingSendMode mode) throws IOException {
            for (int index = 0; index < LOOPS; index++) {
                loops.add(group.next());
            }
            Map<Object, EventLoop> loopOf = new HashMap<>();
            for (int index = 0; index < count; index++) {
                UUID viewer = new UUID(0x9196L, index);
                Object channel = new Object();
                viewers.add(viewer);
                channels.put(viewer, channel);
                loopOf.put(channel, loops.get(index % LOOPS));
            }
            ChannelAccess access = new ChannelAccess() {
                @Override
                public Object channelOf(UUID player) {
                    return channels.get(player);
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
                    submits.incrementAndGet();
                    loopOf.get(channel).execute(task);
                }

                @Override
                public Object loopOf(Object channel) {
                    return loopOf.get(channel);
                }
            };
            reachable = new ReachableChannels(access, new PacketFaultLog(TestLogs.capturing(new CapturingLogSink(),
                    new ManualClock(0L), false)));
            delivery = mode.delivery(reachable, (channel, ping) -> written.incrementAndGet());
        }

        long play(int ticks) throws Exception {
            long spent = 0;
            for (int tick = 0; tick < ticks; tick++) {
                long began = System.nanoTime();
                delivery.openBurst();
                for (UUID viewer : viewers) {
                    Object channel = reachable.channelOrNull(viewer);
                    if (channel != null) {
                        delivery.deliver(channel, id++);
                    }
                }
                delivery.closeBurst();
                spent += System.nanoTime() - began;
                LockSupport.parkNanos(IDLE_BETWEEN_TICKS_NANOS);
            }
            return spent;
        }

        void drain() throws Exception {
            for (EventLoop loop : loops) {
                loop.submit(() -> { }).sync();
            }
        }
    }
}
