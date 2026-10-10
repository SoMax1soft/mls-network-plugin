// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ConsoleLogTest {

    private final CapturingLogSink sink = new CapturingLogSink();
    private final ManualClock clock = new ManualClock(0);
    private final SecretRedactor redactor = new SecretRedactor();
    private final AtomicBoolean debug = new AtomicBoolean();
    private final ConsoleLog log = new ConsoleLog(sink, redactor, debug::get, clock);

    @Test
    void repeatedWarningsAreFoldedForFiveMinutes() {
        log.warn(LogTopic.BACKEND, "request failed after 120 ms");
        log.warn(LogTopic.BACKEND, "request failed after 480 ms");
        log.warn(LogTopic.BACKEND, "request failed after 7 ms");
        clock.advanceMillis(5 * 60 * 1000);
        log.warn(LogTopic.BACKEND, "request failed after 9 ms");
        assertEquals(List.of("[Backend] request failed after 120 ms",
                "[Backend] request failed after 9 ms (repeated 2 times)"), sink.texts(LogLevel.WARNING));
    }

    @Test
    void apiKeyNeverReachesTheConsole() {
        redactor.register("sk-live-123456");
        log.info(LogTopic.BACKEND, "key sk-live-123456 rejected");
        log.error(LogTopic.BACKEND, "failed", new IllegalStateException("url?key=sk-live-123456"));
        assertFalse(sink.lines().stream().anyMatch(line -> line.text().contains("sk-live")));
        assertFalse(String.valueOf(sink.lines().get(1).trace().getMessage()).contains("sk-live"));
        assertTrue(sink.contains(SecretRedactor.MASK));
    }

    @Test
    void debugLinesAreBuiltOnlyInDebugMode() {
        AtomicBoolean built = new AtomicBoolean();
        log.debug(LogTopic.DATA, () -> {
            built.set(true);
            return "x";
        });
        assertFalse(built.get());
        debug.set(true);
        log.debug(LogTopic.DATA, () -> "saved");
        assertEquals(List.of("[Debug] [Data] saved"), sink.texts(LogLevel.DEBUG));
    }

    @Test
    void stackTracesAreThrottledPerCause() {
        IllegalStateException failure = new IllegalStateException("x");
        log.error(LogTopic.PACKETS, "first", failure);
        log.error(LogTopic.PACKETS, "second", failure);
        assertTrue(sink.lines().get(0).trace() != null);
        assertEquals(null, sink.lines().get(1).trace());
    }
}
