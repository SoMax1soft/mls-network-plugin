// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.detection.RestoreResult;
import italy.mlsac.net.testkit.TestCore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PipelineAssemblyTest {

    @Test
    void startsAndOffersItsPorts() {
        PipelineAssembly pipeline = new PipelineAssembly(new TestCore().services());
        pipeline.start();
        assertEquals("pipeline", pipeline.name());
        assertEquals(RestoreResult.NOTHING_STORED, pipeline.ports().history().export(UUID.randomUUID()));
        pipeline.stop();
    }
}
