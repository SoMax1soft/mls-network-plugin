// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PredictionStreamTest {

    @Test
    void readsNdjsonUntilDone() throws PredictionFailure {
        List<Prediction> predictions = PredictionStream.ULTRA.read(
                "{\"type\":\"prediction\",\"probability\":0.951,\"model\":\"pulse\"}\n\n{\"type\":\"error\"}\n"
                        + "{\"type\":\"done\"}\n{\"probability\":0.1}\n");
        assertEquals(List.of(new Prediction("ultra", 0.951)), predictions);
    }

    @Test
    void legacyAnswersKeepTheirModelNames() throws PredictionFailure {
        List<Prediction> predictions = PredictionStream.LEGACY.read(
                "{\"success\":true,\"model\":\"pulse\",\"probability\":0.25}");
        assertEquals(List.of(new Prediction("pulse", 0.25)), predictions);
    }

    @Test
    void errorFieldFailsTheRequest() {
        assertThrows(PredictionFailure.class,
                () -> PredictionStream.ULTRA.read("{\"probability\":0.5,\"error\":\"INVALID_SEQUENCE:40\"}"));
    }
}
