// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiveGoldenReplayTest {

    @ParameterizedTest
    @CsvSource({
        "1.16.5, duel-aura", "1.16.5, duel-legit", "1.16.5, brawl", "1.16.5, range-inout",
        "1.20.4, duel-aura", "1.20.4, duel-legit", "1.20.4, brawl", "1.20.4, range-inout",
        "1.21.4, duel-aura", "1.21.4, duel-legit", "1.21.4, brawl", "1.21.4, range-inout"})
    void reproducesEveryRecordedOutput(String version, String scenario) throws IOException {
        Path trace = GoldenTraces.locateLive(version, scenario);
        ParityReport report = new ReplayDriver(trace).run();
        GoldenTraces.store(GoldenTraces.liveName(version, scenario), report);
        assertTrue(report.complete(), report::render);
    }
}
