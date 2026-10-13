// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import italy.mlsac.net.module.PipelinePorts;
import italy.mlsac.net.detection.AimProfiles;
import italy.mlsac.net.detection.CombatStatus;
import italy.mlsac.net.detection.TickRestore;
import italy.mlsac.net.testkit.TestCore;
import org.junit.jupiter.api.Test;

class FeaturesAssemblyTest {

    @Test
    void startsAndOffersItsPorts() {
        TestCore core = new TestCore();
        PipelinePorts pipeline = new PipelinePorts(TickRestore.NONE, AimProfiles.NONE,
                CombatStatus.NONE);
        FeaturesAssembly features = new FeaturesAssembly(core.services(), pipeline);
        features.start();
        assertEquals("features", features.name());
        assertNotNull(features.ports().penalties());
        features.stop();
    }
}
