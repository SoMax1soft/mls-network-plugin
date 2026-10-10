// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.ManualScheduler;
import italy.mlsac.net.testkit.ScriptedTransport;
import italy.mlsac.net.testkit.TestLogs;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

    private final ManualScheduler scheduler = new ManualScheduler();
    private final ScriptedTransport transport = new ScriptedTransport();
    private final CapturingLogSink sink = new CapturingLogSink();

    private ReleaseWatcher checker(String endpoint, String version) {
        return new ReleaseWatcher(scheduler, transport, TestLogs.capturing(sink, new ManualClock(0), false), endpoint,
                version, 21, () -> true);
    }

    @Test
    void buildsTheQueryAndAnnouncesEachVersionOnce() {
        ReleaseWatcher checker = checker("https://api.mlsac.net/", "2.0.57");
        assertEquals("https://api.mlsac.net/api/v1/plugin/update?java=21&version=2.0.57",
                checker.requestUri().toString());
        String answer = "{\"success\":true,\"data\":{\"version\":\"2.0.999\",\"updateAvailable\":true}}";
        transport.otherwise(200, answer);
        checker.start();
        scheduler.runPending();
        scheduler.advance(ReleaseWatcher.CADENCE_TICKS * 5);
        assertEquals(6, transport.calls().size());
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
        assertEquals(Optional.of("2.0.999"), checker.availableVersion());
        assertEquals("MLSAC-Plugin-Updater/2.0.57", transport.calls().get(0).call().headers().get(0).value());
    }

    @Test
    void devBuildsAndPlainHttpNeverCheck() {
        checker("https://api.mlsac.net", "2.0.dev").start();
        scheduler.advance(10_000);
        assertTrue(transport.calls().isEmpty());
        checker("http://api.mlsac.net", "2.0.5").start();
        scheduler.advance(10_000);
        assertTrue(transport.calls().isEmpty());
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
    }
}
