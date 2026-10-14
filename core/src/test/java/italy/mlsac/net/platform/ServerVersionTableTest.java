// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerVersionTableTest {

    private final ServerVersionTable table = new ServerVersionTable();

    @ParameterizedTest
    @CsvSource({
            "1.16.5-R0.1-SNAPSHOT, 1.16.5",
            "1.17.1, 1.17",
            "1.19.4, 1.19",
            "1.20.4, 1.20",
            "1.20.4-R0.1-SNAPSHOT, 1.20",
            "1.20.6, 1.20.5",
            "1.21.3, 1.21.1",
            "1.21.11, 1.21.11",
            "1.21.8-R0.1-SNAPSHOT, 1.21.8",
            "26.1, 1.21.11",
            "26.2.build.133-stable, 1.21.11",
            "26.1.2.build.8-alpha, 1.21.11",
            "26.2-R0.1-SNAPSHOT, 1.21.11",
            "1.21.11.build.14-stable, 1.21.11",
            "1.22, 1.21.11"
    })
    void mapsKnownVersions(String raw, String expected) {
        VersionResolution resolution = table.resolve(raw);
        assertTrue(resolution.recognised());
        assertEquals(expected, resolution.version().toString());
        assertEquals(List.of(), resolution.warnings());
    }

    @ParameterizedTest
    @CsvSource({
            "1.16.5-R0.1-SNAPSHOT, 1.16.5",
            "1.17.1-R0.1-SNAPSHOT, 1.17.1",
            "1.19.4-R0.1-SNAPSHOT, 1.19.4",
            "1.20.6-R0.1-SNAPSHOT, 1.20.6",
            "1.21.3, 1.21.3",
            "26.2.build.133-stable, 26.2",
            "26.1.2.build.74-stable, 26.1.2",
            "26.3.build.181-stable, 26.3",
            "abc, abc"
    })
    void reportsTheRunningVersionNotTheRulesBucket(String raw, String detected) {
        assertEquals(detected, table.resolve(raw).detected());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankVersionReportsTheFallbackRules(String raw) {
        assertEquals("1.16.5", table.resolve(raw).detected());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.9", "abc", "1", "", "1.x.2"})
    void unknownVersionsRunAsMinimum(String raw) {
        VersionResolution resolution = table.resolve(raw);
        assertFalse(resolution.recognised());
        assertEquals(GameVersion.V1_16_5, resolution.version());
        assertEquals(List.of("Unrecognised server version '" + raw + "', using the 1.16.5 rules"),
                resolution.warnings());
    }

    @ParameterizedTest
    @CsvSource({"1.16.5, LEGACY", "1.17, LEGACY_MODERN", "1.20, LEGACY_MODERN", "1.20.5, MODERN", "1.21.11, MODERN"})
    void compatibilityModes(String version, CompatibilityMode mode) {
        assertEquals(mode, CompatibilityMode.of(GameVersion.parse(version).orElseThrow()));
    }
}
