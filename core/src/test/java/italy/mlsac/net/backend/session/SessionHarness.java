// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.BackendAddress;
import italy.mlsac.net.backend.request.BackendBodies;
import italy.mlsac.net.backend.request.BackendHeaders;
import italy.mlsac.net.backend.request.RequestIdentity;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.ServerEnvironment;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.ManualScheduler;
import italy.mlsac.net.testkit.ScriptedRandom;
import italy.mlsac.net.testkit.ScriptedTransport;
import italy.mlsac.net.testkit.TestLogs;
import java.util.Optional;
import java.util.concurrent.Executor;

final class SessionHarness {

    static final String PRIMARY = "http://inference.test";
    static final String RESERVE = "http://reserve.test";

    final ManualScheduler scheduler = new ManualScheduler();
    final ManualClock clock = new ManualClock(1_789_000_000_000L);
    final ScriptedRandom random = new ScriptedRandom(0);
    final ScriptedTransport transport = new ScriptedTransport();
    final CapturingLogSink sink = new CapturingLogSink();
    final ConsoleLog log = TestLogs.capturing(sink, clock, false);
    final BackendSession session;

    static final String VERSION = "2.0.dev";
    static final String KEY = "TEST-KEY";
    static final RequestIdentity IDENTITY = new RequestIdentity(VERSION, "lobby", "net", "127.0.0.1", 25565,
            false, Optional.empty());

    SessionHarness(boolean withReserve) {
        this(withReserve, Runnable::run);
    }

    SessionHarness(boolean withReserve, Executor encoder) {
        this.session = new BackendSession(contextFor(withReserve ? BackendAddress.of(PRIMARY, RESERVE)
                : BackendAddress.of(PRIMARY, ""), encoder));
    }

    private BackendContext contextFor(BackendAddress address, Executor encoder) {
        return new BackendContext(address, IDENTITY, new BackendHeaders(VERSION, KEY), new BackendBodies(),
                transport, scheduler, clock, random, new BackendConsole(log, clock), () -> 2, ReplyInspector.NONE,
                KEY, ServerEnvironment.UNKNOWN, () -> ClientDistribution.EMPTY, encoder);
    }

    static String init() {
        return "{\"sessionId\":\"s-1\",\"success\":true}";
    }
}
