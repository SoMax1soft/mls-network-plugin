// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.platform.ServerSoftware;
import italy.mlsac.net.platform.ServerVersionTable;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestCore;
import java.util.List;
import org.junit.jupiter.api.Test;

class StartupSummaryTest {

    private final Edition edition = new PluginEdition(new TestCore().services());

    @Test
    void summaryFitsFourLines() {
        PlatformProfile paper = new PlatformProfile(ServerSoftware.PAPER,
                new ServerVersionTable().resolve("1.21.4-R0.1-SNAPSHOT"), true, 21);
        EffectiveConfig config = EffectiveConfig.pluginDefaults(
                TestConfigs.core("detection:\n  api-key: abcd-1234\nremote-config:\n  preset: pvp\n"));
        List<String> lines = new StartupSummary().lines("2.0.57", 120, paper, config, "https://api.mlsac.net",
                new StartupSummary.Integrations(true, false), edition);
        List<String> expected = List.of(
                "MLSAC 2.0.57 enabled in 120 ms: Paper 1.21.4 (Modern (1.20.5+)), Java 21",
                "Detection: on (alert threshold 0.80) | preset: pvp | backend: https://api.mlsac.net (connecting...)",
                edition.featureLine("on"),
                "Integrations: WorldGuard on, Geyser/Floodgate off");
        assertEquals(expected, lines);
    }

    @Test
    void firstLineNamesTheRunningVersion() {
        EffectiveConfig config = EffectiveConfig.pluginDefaults(TestConfigs.core("detection:\n  api-key: abcd-1234\n"));
        PlatformProfile paper26 = new PlatformProfile(ServerSoftware.PAPER,
                new ServerVersionTable().resolve("26.2.build.133-stable"), true, 25);
        PlatformProfile paper119 = new PlatformProfile(ServerSoftware.PAPER,
                new ServerVersionTable().resolve("1.19.4-R0.1-SNAPSHOT"), true, 17);
        assertEquals("MLSAC 2.0.dev enabled in 9 ms: Paper 26.2 (Modern (1.20.5+)), Java 25",
                firstLine(paper26, config));
        assertEquals("MLSAC 2.0.dev enabled in 9 ms: Paper 1.19.4 (Legacy-Modern (1.17-1.20.4)), Java 17",
                firstLine(paper119, config));
    }

    private String firstLine(PlatformProfile platform, EffectiveConfig config) {
        return new StartupSummary().lines("2.0.dev", 9, platform, config, "https://api.mlsac.net",
                new StartupSummary.Integrations(false, false), edition).get(0);
    }

    @Test
    void missingKeyAndFoliaAreExplained() {
        PlatformProfile folia = new PlatformProfile(ServerSoftware.FOLIA,
                new ServerVersionTable().resolve("1.20.4"), false, 17);
        List<String> lines = new StartupSummary().lines("2.0.dev", 5, folia, TestConfigs.defaults(),
                "https://api.mlsac.net", new StartupSummary.Integrations(false, false), edition);
        assertEquals(3, lines.size());
        String detection = lines.get(1);
        String collection = lines.get(lines.size() - 1);
        assertTrue(detection.startsWith("Detection: off (API key default placeholder) | preset: auto"), detection);
        assertTrue(collection.endsWith("off on Folia"), collection);
    }
}
