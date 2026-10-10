// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoldenReplayTest {

    @ParameterizedTest
    @ValueSource(strings = {"ultra_legit_duel", "ultra_aura_vs_legit", "legacy_mixed", "ultra_mixed_outages",
        "ultra_threaded_http"})
    void reproducesEveryRecordedOutput(String scenario) throws IOException {
        Path trace = GoldenTraces.locate(scenario);
        ParityReport report = new ReplayDriver(trace).run();
        GoldenTraces.store(scenario, report);
        assertTrue(report.complete(), report::render);
    }
}
