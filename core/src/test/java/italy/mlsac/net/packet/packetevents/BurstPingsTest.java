// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BurstPingsTest {

    private final CapturingLogSink sink = new CapturingLogSink();
    private final PacketFaultLog faults = new PacketFaultLog(TestLogs.capturing(sink, new ManualClock(0L), false));
    private final StandInLoops access = new StandInLoops();
    private final ReachableChannels channels = new ReachableChannels(access, faults);
    private final List<String> written = Collections.synchronizedList(new ArrayList<>());
    private final BurstPings pings = new BurstPings(channels, (channel, id) -> written.add(channel + ":" + id));

    @Test
    void aBurstHandsEachLoopOneTaskWithItsPingsInSendOrder() {
        StandInLoops.Loop first = new StandInLoops.Loop("first");
        StandInLoops.Loop second = new StandInLoops.Loop("second");
        Object a = access.channelOn(first, "a");
        Object b = access.channelOn(second, "b");
        Object c = access.channelOn(first, "c");
        Object d = access.channelOn(second, "d");
        pings.openBurst();
        pings.deliver(a, 1);
        pings.deliver(b, 2);
        pings.deliver(c, 3);
        pings.deliver(d, 4);
        assertEquals(0, first.queued());
        assertEquals(0, second.queued());
        pings.closeBurst();
        assertEquals(1, first.queued());
        assertEquals(1, second.queued());
        assertTrue(written.isEmpty());
        first.runAll();
        assertEquals(List.of("a:1", "c:3"), written);
        second.runAll();
        assertEquals(List.of("a:1", "c:3", "b:2", "d:4"), written);
    }

    @Test
    void outsideABurstEveryPingIsItsOwnLoopTask() {
        StandInLoops.Loop loop = new StandInLoops.Loop("only");
        Object a = access.channelOn(loop, "a");
        pings.deliver(a, 1);
        pings.deliver(a, 2);
        assertEquals(2, loop.queued());
        loop.runAll();
        assertEquals(List.of("a:1", "a:2"), written);
    }

    @Test
    void anotherThreadDuringABurstSendsAtOnce() throws InterruptedException {
        StandInLoops.Loop loop = new StandInLoops.Loop("only");
        Object a = access.channelOn(loop, "a");
        Object b = access.channelOn(loop, "b");
        pings.openBurst();
        pings.deliver(a, 1);
        Thread region = new Thread(() -> {
            pings.openBurst();
            pings.deliver(b, 7);
            pings.closeBurst();
        });
        region.start();
        region.join();
        assertEquals(1, loop.queued());
        pings.closeBurst();
        assertEquals(2, loop.queued());
        loop.runAll();
        assertEquals(List.of("b:7", "a:1"), written);
    }

    @Test
    void aChannelWithoutAKnownLoopIsHandedOverAloneAndTheBurstGoesOn() {
        StandInLoops.Loop loop = new StandInLoops.Loop("only");
        Object a = access.channelOn(loop, "a");
        Object broken = access.channelWithBrokenLoop("broken");
        pings.openBurst();
        pings.deliver(broken, 5);
        pings.deliver(a, 6);
        pings.closeBurst();
        assertTrue(sink.contains("finding a channel's network thread"));
        assertTrue(sink.contains("handing a transaction ping to a player's network thread"));
        assertEquals(1, loop.queued());
        loop.runAll();
        assertEquals(List.of("a:6"), written);
    }

    @Test
    void aClosedBurstStartsEmptyAndCloseWithoutOpenDoesNothing() {
        StandInLoops.Loop loop = new StandInLoops.Loop("only");
        Object a = access.channelOn(loop, "a");
        pings.closeBurst();
        pings.openBurst();
        pings.deliver(a, 1);
        pings.closeBurst();
        pings.openBurst();
        pings.closeBurst();
        assertEquals(1, loop.queued());
        loop.runAll();
        assertEquals(List.of("a:1"), written);
    }

    @Test
    void largeBurstsKeepEveryPing() {
        StandInLoops.Loop loop = new StandInLoops.Loop("only");
        List<String> expected = new ArrayList<>();
        pings.openBurst();
        for (int i = 0; i < 100; i++) {
            Object channel = access.channelOn(loop, "c" + i);
            pings.deliver(channel, i);
            expected.add("c" + i + ":" + i);
        }
        pings.closeBurst();
        assertEquals(1, loop.queued());
        loop.runAll();
        assertEquals(expected, written);
    }
}
