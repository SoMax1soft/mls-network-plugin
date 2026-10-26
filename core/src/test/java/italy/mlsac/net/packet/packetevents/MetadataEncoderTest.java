// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import italy.mlsac.net.packet.VirtualPose;
import org.junit.jupiter.api.Test;

class MetadataEncoderTest {

    @Test
    void everyVirtualPoseHasAPacketEventsPose() {
        for (VirtualPose pose : VirtualPose.values()) {
            assertEquals(pose.name(), EntityPose.valueOf(pose.name()).name());
        }
    }
}
