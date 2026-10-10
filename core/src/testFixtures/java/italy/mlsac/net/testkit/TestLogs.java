// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.console.SecretRedactor;

public final class TestLogs {

    private TestLogs() {
    }

    public static ConsoleLog capturing(CapturingLogSink sink, Clock clock, boolean debug) {
        return new ConsoleLog(sink, new SecretRedactor(), () -> debug, clock);
    }
}
