// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.testkit.TestConfigs;
import org.junit.jupiter.api.Test;

class WallChecksSettingTest {

    @Test
    void wallChecksRunTenTimesASecondUnlessTwentyIsAskedFor() {
        assertEquals(10, TestConfigs.core("debug: false\n").wallChecksPerSecond());
        assertEquals(20, TestConfigs.core("performance:\n  wall-checks-per-second: 20\n").wallChecksPerSecond());
        assertEquals(10, TestConfigs.core("performance:\n  wall-checks-per-second: 10\n").wallChecksPerSecond());
        assertEquals(10, TestConfigs.core("performance:\n  wall-checks-per-second: 15\n").wallChecksPerSecond());
        assertEquals(10, TestConfigs.core("performance:\n  wall-checks-per-second: fast\n").wallChecksPerSecond());
    }
}
