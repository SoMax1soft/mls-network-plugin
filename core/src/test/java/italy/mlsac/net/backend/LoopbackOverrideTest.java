// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.console.SecretRedactor;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoopbackOverrideTest {

    @Test
    void withoutPropertyTheFixedAddressesStay() {
        LoopbackOverride none = new LoopbackOverride(null);
        assertFalse(none.accepted());
        assertEquals(BackendAddress.production(), none.address());
        assertEquals(BackendAddress.production(), new LoopbackOverride("  ").address());
    }

    @Test
    void loopbackAddressReplacesBothEndpoints() {
        LoopbackOverride local = new LoopbackOverride("http://127.0.0.1:18080/");
        assertTrue(local.accepted());
        assertEquals("http://127.0.0.1:18080", local.address().primary());
        assertEquals(Optional.empty(), local.address().reserve());
        assertTrue(new LoopbackOverride("https://localhost:8443").accepted());
        assertTrue(new LoopbackOverride("http://[::1]:18080").accepted());
    }

    @Test
    void remoteOrMalformedAddressesAreRefused() {
        for (String refused : new String[]{"http://example.org", "http://127.0.0.2:80", "ftp://127.0.0.1",
                "http://user@127.0.0.1:1", "127.0.0.1:18080", "http://127.0.0.1.evil.test", "::not a uri"}) {
            LoopbackOverride override = new LoopbackOverride(refused);
            assertFalse(override.accepted(), refused);
            assertEquals(BackendAddress.production(), override.address(), refused);
        }
    }

    @Test
    void resolvingWarnsOnceOnlyWhenThePropertyIsSet() {
        CapturingLogSink sink = new CapturingLogSink();
        ConsoleLog log = new ConsoleLog(sink, new SecretRedactor(), () -> false, new ManualClock(0));
        assertEquals(BackendAddress.production(), new LoopbackOverride(null).resolve(log));
        assertEquals(List.of(), sink.lines());
        assertEquals("http://127.0.0.1:18080", new LoopbackOverride("http://127.0.0.1:18080").resolve(log).primary());
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
        assertTrue(sink.contains("TEST MODE"));
        assertTrue(sink.contains(LoopbackOverride.PROPERTY));
        assertEquals(BackendAddress.production(), new LoopbackOverride("http://example.org").resolve(log));
        assertEquals(2, sink.texts(LogLevel.WARNING).size());
        assertTrue(sink.contains("ignored"));
    }
}
