// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.player.BedrockDetector;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReflectiveBedrockDetectorTest {

    private static final UUID FLOODGATE = UUID.fromString("00000000-0000-0000-0009-01f1221ee010");
    private static final UUID MOJANG = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID OFFLINE = UUID.nameUUIDFromBytes("OfflinePlayer:Steve".getBytes(StandardCharsets.UTF_8));

    @Test
    void withoutFloodgateOrGeyserTheUuidStillMarksBedrockPlayers() {
        BedrockDetector detector = ReflectiveBedrockDetector.detect(getClass().getClassLoader());
        assertSame(BedrockDetector.FLOODGATE_UUID, detector);
        assertTrue(detector.bedrockClient(FLOODGATE));
        assertFalse(detector.bedrockClient(MOJANG));
        assertFalse(detector.bedrockClient(OFFLINE));
        assertFalse(detector.bedrockClient(null));
    }

    @Test
    void javaUuidsAreNeverTakenForFloodgate() {
        for (int i = 0; i < 1000; i++) {
            assertFalse(BedrockDetector.floodgateUuid(UUID.randomUUID()));
            assertFalse(BedrockDetector.floodgateUuid(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:p" + i).getBytes(StandardCharsets.UTF_8))));
        }
    }
}
