// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.ultra.MlsuWire;
import java.util.Base64;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoldenWindowHeaderTest {

    @ParameterizedTest
    @ValueSource(strings = {"ultra_legit_duel", "ultra_aura_vs_legit"})
    void everyRecordedWindowIsVersionFourSixtyByFiftyByFourteen(String scenario) {
        int[] windows = {0};
        new TraceFile(GoldenTraces.locate(scenario)).forEachEvent(event -> {
            for (TraceRecord output : event.outputs()) {
                if ("ng.window".equals(output.name())) {
                    byte[] bytes = Base64.getDecoder().decode(output.text("b64"));
                    assertEquals(new MlsuWire.MlsuHeader(4, 60, 50, 14, 15_380), MlsuWire.header(bytes));
                    windows[0]++;
                }
            }
        });
        assertTrue(windows[0] > 0);
    }
}
