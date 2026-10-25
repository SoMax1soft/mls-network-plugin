// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReachableChannelsTest {

    private static final UUID PLAYER = new UUID(7, 7);

    private final CapturingLogSink sink = new CapturingLogSink();
    private final PacketFaultLog faults = new PacketFaultLog(TestLogs.capturing(sink, new ManualClock(0L), false));
    private final ExecutorService loop = Executors.newSingleThreadExecutor();

    @AfterEach
    void stopLoop() {
        loop.shutdownNow();
    }

    @Test
    void injectedOpenChannelIsReachable() {
        StandInChannel channel = StandInChannel.injected(loop);
        ReachableChannels channels = new ReachableChannels(channel.access(), faults);
        assertSame(channel, channels.channelOrNull(PLAYER));
        assertTrue(channels.carries(channel));
    }

    @Test
    void injectedChannelIsCheckedForHandlersOnlyOnce() {
        StandInChannel channel = StandInChannel.injected(loop);
        ReachableChannels channels = new ReachableChannels(channel.access(), faults);
        for (int tick = 0; tick < 50; tick++) {
            assertSame(channel, channels.channelOrNull(PLAYER));
        }
        assertEquals(1, channel.injectionChecks());
    }

    @Test
    void channelWithoutHandlersIsCheckedAgainUntilInjected() {
        StandInChannel channel = StandInChannel.injected(loop).withoutHandlers();
        ReachableChannels channels = new ReachableChannels(channel.access(), faults);
        assertNull(channels.channelOrNull(PLAYER));
        assertNull(channels.channelOrNull(PLAYER));
        assertEquals(2, channel.injectionChecks());
    }

    @Test
    void channelWithoutPacketHandlersIsNeverWrittenOrScheduled() {
        StandInChannel channel = StandInChannel.injected(loop).withoutHandlers();
        ReachableChannels channels = new ReachableChannels(channel.access(), faults);
        AtomicInteger ran = new AtomicInteger();
        assertNull(channels.channelOrNull(PLAYER));
        assertFalse(channels.runOnLoop(PLAYER, ran::incrementAndGet));
        assertEquals(0, channel.scheduled());
        assertEquals(0, ran.get());
    }

    @Test
    void closedOrMissingChannelsAreUnreachable() {
        assertNull(new ReachableChannels(StandInChannel.injected(loop).closed().access(), faults).channelOrNull(PLAYER));
        assertNull(new ReachableChannels(StandInChannel.absent(), faults).channelOrNull(PLAYER));
        assertFalse(new ReachableChannels(StandInChannel.absent(), faults).runOnLoop(PLAYER, () -> { }));
    }

    @Test
    void terminatedEventLoopRefusesWithoutThrowing() {
        loop.shutdown();
        ReachableChannels channels = new ReachableChannels(StandInChannel.injected(loop).access(), faults);
        assertFalse(assertDoesNotThrow(() -> channels.runOnLoop(PLAYER, () -> { })));
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
    }

    @Test
    void failingTaskLeavesTheEventLoopAlive() throws InterruptedException {
        StandInChannel channel = StandInChannel.injected(loop);
        ReachableChannels channels = new ReachableChannels(channel.access(), faults);
        CountDownLatch after = new CountDownLatch(1);
        assertTrue(channels.runOnLoop(PLAYER, () -> {
            throw new NullPointerException("no handler context");
        }));
        assertTrue(channels.runOnLoop(PLAYER, () -> {
            throw new IllegalStateException("second failure");
        }));
        assertTrue(channels.runOnLoop(PLAYER, () -> {
            throw new NullPointerException("folded");
        }));
        assertTrue(channels.runOnLoop(PLAYER, after::countDown));
        assertTrue(after.await(5, TimeUnit.SECONDS));
        assertEquals(4, channel.scheduled());
        assertEquals(0, channel.escaped());
        assertEquals(2, sink.texts(LogLevel.WARNING).size());
        assertEquals(1, faults.suppressedCount());
    }

    @Test
    void failingWritesAreContainedAndLogged() {
        ReachableChannels channels = new ReachableChannels(StandInChannel.injected(loop).access(), faults);
        assertDoesNotThrow(() -> channels.attempt("writing a virtual entity packet", () -> {
            throw new NullPointerException("context(String) is null");
        }));
        assertTrue(sink.contains("writing a virtual entity packet"));
    }

    @Test
    void brokenChannelChecksCountAsUnreachable() {
        StandInChannel channel = StandInChannel.injected(loop).withFailingCheck();
        assertNull(new ReachableChannels(channel.access(), faults).channelOrNull(PLAYER));
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
    }

    @Test
    void closedChannelsAreForgottenOnceManyHaveBeenSeen() {
        Map<UUID, Object> byPlayer = new ConcurrentHashMap<>();
        Set<Object> closed = ConcurrentHashMap.newKeySet();
        ChannelAccess access = new ChannelAccess() {
            @Override
            public Object channelOf(UUID player) {
                return byPlayer.get(player);
            }

            @Override
            public boolean open(Object channel) {
                return !closed.contains(channel);
            }

            @Override
            public boolean injected(Object channel) {
                return true;
            }

            @Override
            public void execute(Object channel, Runnable task) {
                task.run();
            }

            @Override
            public Object loopOf(Object channel) {
                return this;
            }
        };
        ReachableChannels channels = new ReachableChannels(access, faults);
        for (int session = 0; session < 1000; session++) {
            UUID player = new UUID(9, session);
            Object channel = new Object();
            byPlayer.put(player, channel);
            assertSame(channel, channels.channelOrNull(player));
            closed.add(channel);
            byPlayer.remove(player);
        }
        UUID stays = new UUID(10, 1);
        Object open = new Object();
        byPlayer.put(stays, open);
        assertSame(open, channels.channelOrNull(stays));
        assertTrue(channels.remembered() < 70, "remembered " + channels.remembered());
        assertSame(open, channels.channelOrNull(stays));
    }

    @Test
    void aPingRouteKeepsItsChannelWhileItIsOpen() {
        Map<UUID, Object> byPlayer = new ConcurrentHashMap<>();
        Set<Object> closed = ConcurrentHashMap.newKeySet();
        AtomicInteger lookups = new AtomicInteger();
        ChannelAccess access = new ChannelAccess() {
            @Override
            public Object channelOf(UUID player) {
                lookups.incrementAndGet();
                return byPlayer.get(player);
            }

            @Override
            public boolean open(Object channel) {
                return !closed.contains(channel);
            }

            @Override
            public boolean injected(Object channel) {
                return true;
            }

            @Override
            public void execute(Object channel, Runnable task) {
                task.run();
            }

            @Override
            public Object loopOf(Object channel) {
                return this;
            }
        };
        ReachableChannels channels = new ReachableChannels(access, faults);
        Object first = new Object();
        byPlayer.put(PLAYER, first);
        ChannelRoute route = new ChannelRoute();
        for (int tick = 0; tick < 20; tick++) {
            assertSame(first, channels.routed(route, PLAYER));
        }
        assertEquals(1, lookups.get());
        Object second = new Object();
        closed.add(first);
        byPlayer.put(PLAYER, second);
        assertSame(second, channels.routed(route, PLAYER));
        closed.add(second);
        byPlayer.remove(PLAYER);
        assertNull(channels.routed(route, PLAYER));
    }
}
