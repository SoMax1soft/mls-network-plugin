// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SynchronousEventBusTest {

    interface Marker {
    }

    record Ping(int value) implements Marker {
    }

    @Test
    void deliversToExactTypeAndInterfacesAndIsolatesFailures() {
        CapturingLogSink sink = new CapturingLogSink();
        SynchronousEventBus bus = new SynchronousEventBus(TestLogs.capturing(sink, new ManualClock(0), false));
        List<String> seen = new ArrayList<>();
        bus.subscribe(Ping.class, ping -> {
            throw new IllegalStateException("boom");
        });
        Subscription exact = bus.subscribe(Ping.class, ping -> seen.add("ping" + ping.value()));
        bus.subscribe(Marker.class, marker -> seen.add("marker"));
        bus.publish(new Ping(1));
        exact.cancel();
        bus.publish(new Ping(2));
        assertEquals(List.of("ping1", "marker", "marker"), seen);
        assertEquals(1, sink.texts(LogLevel.ERROR).size());
        assertTrue(sink.texts(LogLevel.ERROR).get(0).contains("Ping"));
    }
}
