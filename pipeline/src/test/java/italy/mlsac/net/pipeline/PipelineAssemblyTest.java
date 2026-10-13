// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.detection.RestoreLabel;
import italy.mlsac.net.detection.RestoreOutcome;
import italy.mlsac.net.detection.RestoreRequest;
import italy.mlsac.net.testkit.TestCore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PipelineAssemblyTest {

    @Test
    void startsAndOffersItsPorts() {
        PipelineAssembly pipeline = new PipelineAssembly(new TestCore().services());
        pipeline.start();
        assertEquals("pipeline", pipeline.name());
        assertEquals(RestoreOutcome.DISABLED, pipeline.ports().restore()
                .restore(new RestoreRequest(UUID.randomUUID(), RestoreLabel.LEGIT, "Mod")).outcome());
        pipeline.stop();
    }
}
