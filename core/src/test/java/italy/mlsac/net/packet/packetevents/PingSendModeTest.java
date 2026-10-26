// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import org.junit.jupiter.api.Test;

class PingSendModeTest {

    @Test
    void namedModesAreRecognised() {
        assertEquals(PingSendMode.BASELINE, PingSendMode.parse("baseline"));
        assertEquals(PingSendMode.LOOP, PingSendMode.parse("loop"));
        assertEquals(PingSendMode.BATCH, PingSendMode.parse(" Batch "));
    }

    @Test
    void missingOrUnknownValuesFallBackToTheDefault() {
        assertEquals(PingSendMode.BATCH, PingSendMode.DEFAULT);
        assertEquals(PingSendMode.DEFAULT, PingSendMode.parse(null));
        assertEquals(PingSendMode.DEFAULT, PingSendMode.parse(""));
        assertEquals(PingSendMode.DEFAULT, PingSendMode.parse("lazy"));
    }

    @Test
    void eachModeBuildsItsOwnDelivery() {
        PacketFaultLog faults = new PacketFaultLog(TestLogs.capturing(new CapturingLogSink(), new ManualClock(0L),
                false));
        ReachableChannels channels = new ReachableChannels(StandInChannel.absent(), faults);
        PingWrite write = (channel, id) -> { };
        assertEquals(DirectPings.class, PingSendMode.BASELINE.delivery(channels, write).getClass());
        assertEquals(LoopPings.class, PingSendMode.LOOP.delivery(channels, write).getClass());
        assertEquals(BurstPings.class, PingSendMode.BATCH.delivery(channels, write).getClass());
    }
}
