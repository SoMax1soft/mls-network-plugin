// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.util.ReferenceCountUtil;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class PingOrderingTest {

    private static final int VIEWERS = 6;
    private static final int TICKS = 40;

    private final EventLoopGroup loops = new DefaultEventLoopGroup(3);
    private final PacketFaultLog faults = new PacketFaultLog(TestLogs.capturing(new CapturingLogSink(),
            new ManualClock(0L), false));

    @AfterEach
    void stopLoops() throws InterruptedException {
        loops.shutdownGracefully(0, 1, TimeUnit.SECONDS).await(5, TimeUnit.SECONDS);
    }

    @Test
    @Timeout(120)
    void everyModeLeavesEachChannelWithTheSameWritesFlushesAndCompletions() throws Exception {
        Map<PingSendMode, List<List<String>>> logs = new EnumMap<>(PingSendMode.class);
        for (PingSendMode mode : PingSendMode.values()) {
            logs.put(mode, playTicks(mode));
        }
        List<List<String>> baseline = logs.get(PingSendMode.BASELINE);
        for (List<String> channelLog : baseline) {
            assertPingsSitBetweenTheirTicks(channelLog);
        }
        for (PingSendMode mode : PingSendMode.values()) {
            assertEquals(baseline, logs.get(mode), mode.key());
        }
    }

    private static void assertPingsSitBetweenTheirTicks(List<String> log) {
        for (int tick = 0; tick < TICKS; tick++) {
            int entity = log.indexOf("write entity " + tick);
            int ping = log.indexOf("write ping " + tick);
            int written = log.indexOf("written ping " + tick);
            int after = log.indexOf("write after " + tick);
            assertTrue(entity >= 0 && entity < ping, "tick " + tick + " " + log);
            assertEquals("flush", log.get(ping + 1), "tick " + tick);
            assertEquals(ping + 2, written, "tick " + tick);
            assertTrue(written < after, "tick " + tick);
            if (tick + 1 < TICKS) {
                assertTrue(after < log.indexOf("write entity " + (tick + 1)), "tick " + tick);
            }
        }
    }

    private List<List<String>> playTicks(PingSendMode mode) throws Exception {
        LocalAddress address = new LocalAddress("ping-order-" + UUID.randomUUID());
        Channel server = new ServerBootstrap().group(loops).channel(LocalServerChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel child) {
                        child.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                ReferenceCountUtil.release(msg);
                            }
                        });
                    }
                }).bind(address).sync().channel();
        List<Channel> viewers = new ArrayList<>();
        List<List<String>> logs = new ArrayList<>();
        Set<Object> usedLoops = new HashSet<>();
        for (int i = 0; i < VIEWERS; i++) {
            List<String> log = Collections.synchronizedList(new ArrayList<>());
            ChannelFuture connected = new Bootstrap().group(loops).channel(LocalChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new WireRecorder(log));
                        }
                    }).connect(address);
            assertTrue(connected.await(10, TimeUnit.SECONDS) && connected.isSuccess());
            Channel viewer = connected.channel();
            viewers.add(viewer);
            logs.add(log);
            usedLoops.add(viewer.eventLoop());
        }
        assertTrue(usedLoops.size() > 1);
        ReachableChannels channels = new ReachableChannels(new LocalChannels(), faults);
        PingDelivery pings = mode.delivery(channels, (channel, id) -> ((Channel) channel).writeAndFlush("ping " + id));
        for (int tick = 0; tick < TICKS; tick++) {
            for (Channel viewer : viewers) {
                String entity = "entity " + tick;
                viewer.eventLoop().execute(() -> viewer.write(entity));
                viewer.write("lazy " + tick);
            }
            for (Channel viewer : viewers) {
                viewer.eventLoop().execute(viewer::flush);
            }
            pings.openBurst();
            for (Channel viewer : viewers) {
                pings.deliver(viewer, tick);
            }
            pings.closeBurst();
            for (Channel viewer : viewers) {
                String after = "after " + tick;
                viewer.eventLoop().execute(() -> viewer.writeAndFlush(after));
            }
        }
        for (Channel viewer : viewers) {
            viewer.eventLoop().submit(() -> { }).sync();
            viewer.close().sync();
        }
        server.close().sync();
        List<List<String>> copies = new ArrayList<>();
        for (List<String> log : logs) {
            synchronized (log) {
                copies.add(new ArrayList<>(log));
            }
        }
        return copies;
    }

    private static final class WireRecorder extends ChannelOutboundHandlerAdapter {
        private final List<String> log;

        WireRecorder(List<String> log) {
            this.log = log;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            String text = String.valueOf(msg);
            log.add("write " + text);
            if (text.startsWith("ping ")) {
                promise.addListener(done -> log.add("written " + text));
            }
            ctx.write(msg, promise);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) {
            log.add("flush");
            ctx.flush();
        }
    }

    private static final class LocalChannels implements ChannelAccess {

        @Override
        public Object channelOf(UUID player) {
            return null;
        }

        @Override
        public boolean open(Object channel) {
            return ((Channel) channel).isOpen();
        }

        @Override
        public boolean injected(Object channel) {
            return true;
        }

        @Override
        public void execute(Object channel, Runnable task) {
            ((Channel) channel).eventLoop().execute(task);
        }

        @Override
        public Object loopOf(Object channel) {
            return ((Channel) channel).eventLoop();
        }
    }
}
