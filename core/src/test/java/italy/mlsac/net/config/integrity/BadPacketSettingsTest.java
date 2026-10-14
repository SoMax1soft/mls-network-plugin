// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.core.CoreSettingsReader;
import italy.mlsac.net.config.local.LocalConfigFile;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.MemoryDataFolder;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.yaml.SnakeYamlParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BadPacketSettingsTest {

    @Test
    void missingSectionMeansTheDocumentedDefaults() {
        BadPacketSettings defaults = TestConfigs.core().badPackets();
        assertEquals(BadPacketSettings.pluginDefaults(), defaults);
        assertTrue(defaults.enabled());
        assertEquals(3, defaults.alertThreshold());
        assertEquals(60_000L, defaults.windowMillis());
        assertEquals(10_000L, defaults.strikeIntervalMillis());
        assertEquals(30, defaults.tickEndsPerSecond());
        assertEquals(200, defaults.tickEndBurst());
    }

    @Test
    void theBundledConfigLeavesTheSectionOutAndTheDefaultsApply() {
        String bundled = new ClasspathResources(getClass().getClassLoader()).text("config.yml").orElseThrow();
        assertFalse(bundled.contains("bad-packets") || bundled.contains("tick-end"));
        CoreSettingsReader.Result read = new CoreSettingsReader().read(TestConfigs.yaml(bundled));
        assertEquals(BadPacketSettings.pluginDefaults(), read.settings().badPackets());
        assertEquals(List.of(), read.warnings());
    }

    @Test
    void localValuesAreReadAndClampedWithWarnings() {
        CoreSettingsReader.Result read = new CoreSettingsReader().read(TestConfigs.yaml("bad-packets:\n"
                + "  enabled: false\n  alert-threshold: 5\n  window-seconds: 120\n  strike-interval-seconds: 0\n"
                + "  tick-end-max-per-second: 20\n  tick-end-burst: lots\n"));
        BadPacketSettings settings = read.settings().badPackets();
        assertFalse(settings.enabled());
        assertEquals(5, settings.alertThreshold());
        assertEquals(120, settings.windowSeconds());
        assertEquals(0, settings.strikeIntervalSeconds());
        assertEquals(25, settings.tickEndsPerSecond());
        assertEquals(BadPacketSettings.DEFAULT_TICK_END_BURST, settings.tickEndBurst());
        assertEquals(2, read.warnings().size());
    }

    @Test
    void aSectionTheOwnerAddedByHandIsKeptAsWrittenAndRead() throws Exception {
        MemoryDataFolder folder = new MemoryDataFolder();
        ConsoleLog log = TestLogs.capturing(new CapturingLogSink(), new ManualClock(0), false);
        LocalConfigFile file = new LocalConfigFile(folder, new ClasspathResources(getClass().getClassLoader()),
                new SnakeYamlParser(), log);
        folder.writeText("config.yml", "_schema-version: '2.0'\nlanguage: en\ndetection:\n  api-key: k-1\n"
                + "bad-packets:\n  alert-threshold: 4\n");
        ConfigTree tree = file.prepare();
        assertEquals(4, tree.value("bad-packets.alert-threshold").orElseThrow());
        assertFalse(tree.contains("bad-packets.enabled"));
        assertFalse(folder.files().get("config.yml").contains("tick-end-burst"));
        assertEquals("k-1", tree.value("detection.api-key").orElseThrow());
        BadPacketSettings settings = new CoreSettingsReader().read(tree).settings().badPackets();
        assertEquals(4, settings.alertThreshold());
        assertTrue(settings.enabled());
        assertEquals(BadPacketSettings.DEFAULT_TICK_END_BURST, settings.tickEndBurst());
    }

    @Test
    void aPresetCannotTouchTheLocalSection() {
        CoreSettings local = TestConfigs.core("bad-packets:\n  alert-threshold: 4\n");
        Map<String, Object> hostile = Map.of("bad-packets", Map.of("enabled", false, "alert-threshold", 99));
        PresetSnapshot snapshot = new PresetSnapshot(Optional.empty(), "x", new PresetLayout().toEffective(hostile));
        EffectiveConfig config = new ConfigAssembler().assemble(local, ConfigTree.empty(), Optional.of(snapshot))
                .config();
        assertEquals(local.badPackets(), config.core().badPackets());
        assertTrue(config.core().badPackets().enabled());
        assertEquals(4, config.core().badPackets().alertThreshold());
    }
}
