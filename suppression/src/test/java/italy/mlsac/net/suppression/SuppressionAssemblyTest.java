// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.detection.DetectionStates;
import italy.mlsac.net.module.FeaturePorts;
import italy.mlsac.net.penalty.PenaltyCommands;
import italy.mlsac.net.player.LagExemptions;
import italy.mlsac.net.staff.StaffChannel;
import italy.mlsac.net.testkit.TestCore;
import org.junit.jupiter.api.Test;

class SuppressionAssemblyTest {

    @Test
    void startsAndStops() {
        SuppressionAssembly suppression = new SuppressionAssembly(new TestCore().services(),
                new FeaturePorts(PenaltyCommands.NONE, StaffChannel.NONE, DetectionStates.NONE), LagExemptions.NONE);
        suppression.start();
        assertEquals("suppression", suppression.name());
        suppression.stop();
    }
}
