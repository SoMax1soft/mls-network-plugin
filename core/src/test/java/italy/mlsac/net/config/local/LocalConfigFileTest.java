// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.MemoryDataFolder;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.yaml.SnakeYamlParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalConfigFileTest {

    private final MemoryDataFolder folder = new MemoryDataFolder();
    private final CapturingLogSink sink = new CapturingLogSink();
    private final ConsoleLog log = TestLogs.capturing(sink, new ManualClock(0), false);
    private final ClasspathResources resources = new ClasspathResources(getClass().getClassLoader());
    private final LocalConfigFile file = new LocalConfigFile(folder, resources, new SnakeYamlParser(), log);

    @Test
    void freshStartWritesOnlyTheBundledConfig() throws Exception {
        ConfigTree tree = file.prepare();
        assertEquals(resources.text("config.yml").orElseThrow(), folder.files().get("config.yml"));
        assertEquals(1, folder.files().size());
        assertEquals("your-api-key", tree.value("detection.api-key").orElseThrow());
        assertFalse(tree.contains("folia"));
        assertFalse(tree.contains("detection.endpoint"));
        assertFalse(tree.contains("outputDirectory"));
    }

    @Test
    void schemaMigrationKeepsOnlyTheConnectionKeys() throws Exception {
        folder.writeText("config.yml", "_schema-version: 'b54'\nlanguage: en\ndebug: true\n"
                + "outputDirectory: custom\ndetection:\n  api-key: \"secret-\\\"key\"\n  endpoint: http://old\n"
                + "server-identity:\n  name: lobby1\n  reporting:\n    alert-threshold: 0.9\n"
                + "violation:\n  threshold: 99\nfolia:\n  enabled: true\n");
        ConfigTree tree = file.prepare();
        assertEquals("en", tree.value("language").orElseThrow());
        assertEquals(true, tree.value("debug").orElseThrow());
        assertEquals("secret-\"key", tree.value("detection.api-key").orElseThrow());
        assertEquals("lobby1", tree.value("server-identity.name").orElseThrow());
        assertEquals(0.9, tree.value("server-identity.reporting.alert-threshold").orElseThrow());
        assertEquals("2.0", tree.value("_schema-version").orElseThrow());
        assertFalse(tree.contains("violation"));
        assertFalse(tree.contains("folia"));
        assertFalse(tree.contains("outputDirectory"));
        assertTrue(sink.contains("config.yml rewritten for layout 2.0 (was b54)"));
    }

    @Test
    void legacyAiKeyIsAdoptedWhenTheNewKeyIsUnset() throws Exception {
        folder.writeText("config.yml", "ai:\n  api-key: legacy-key-99\ndetection:\n  api-key: your-api-key\n");
        assertEquals("legacy-key-99", file.prepare().value("detection.api-key").orElseThrow());
    }

    @Test
    void missingEntriesAreAddedWithoutLosingValuesOrExtraSections() throws Exception {
        folder.writeText("config.yml", "_schema-version: '2.0'\nlanguage: uk\ndetection:\n  api-key: k-1234\n"
                + "violation:\n  threshold: 55\n");
        ConfigTree tree = file.prepare();
        assertEquals("uk", tree.value("language").orElseThrow());
        assertEquals("k-1234", tree.value("detection.api-key").orElseThrow());
        assertEquals(55, tree.value("violation.threshold").orElseThrow());
        assertEquals("default", tree.value("server-identity.name").orElseThrow());
        assertTrue(sink.contains("added missing entries"));
        assertFalse(file.syncMissing());
    }

    @Test
    void retiredFilesMoveToOld() throws Exception {
        folder.writeText("holograms.yml", "x: 1");
        folder.writeText("modules/anti-esp.yml", "y: 2");
        folder.writeText("messages_ru.yml", "z: 3");
        file.prepare();
        Map<String, String> files = folder.files();
        assertTrue(files.containsKey("old/holograms.yml"));
        assertTrue(files.containsKey("old/modules/anti-esp.yml"));
        assertTrue(files.containsKey("old/messages_ru.yml"));
        assertFalse(files.containsKey("holograms.yml"));
    }

    @Test
    void noGeneratedOrRewrittenConfigMentionsBadPackets() throws Exception {
        file.prepare();
        assertNoBadPackets(folder.files().get("config.yml"));
        assertFalse(file.syncMissing());
        assertNoBadPackets(folder.files().get("config.yml"));

        MemoryDataFolder legacyFolder = new MemoryDataFolder();
        legacyFolder.writeText("config.yml", "ai:\n  api-key: legacy-key-99\nlanguage: ru\ndebug: false\n"
                + "violation:\n  threshold: 40\ndetection:\n  endpoint: http://old\n");
        LocalConfigFile legacy = new LocalConfigFile(legacyFolder, resources, new SnakeYamlParser(), log);
        assertEquals("legacy-key-99", legacy.prepare().value("detection.api-key").orElseThrow());
        assertNoBadPackets(legacyFolder.files().get("config.yml"));

        MemoryDataFolder devFolder = new MemoryDataFolder();
        devFolder.writeText("config.yml", "_schema-version: 'b55'\nlanguage: en\ndetection:\n  api-key: k-7\n"
                + "bad-packets:\n  enabled: true\n  alert-threshold: 3\n  tick-end-burst: 200\n");
        LocalConfigFile dev = new LocalConfigFile(devFolder, resources, new SnakeYamlParser(), log);
        dev.prepare();
        assertNoBadPackets(devFolder.files().get("config.yml"));
        assertFalse(sink.contains("bad-packets"));

        MemoryDataFolder partialFolder = new MemoryDataFolder();
        partialFolder.writeText("config.yml", "_schema-version: '2.0'\nlanguage: uk\n");
        LocalConfigFile partial = new LocalConfigFile(partialFolder, resources, new SnakeYamlParser(), log);
        assertTrue(partial.syncMissing());
        assertNoBadPackets(partialFolder.files().get("config.yml"));
        assertEquals("uk", partial.prepare().value("language").orElseThrow());
        assertNoBadPackets(partialFolder.files().get("config.yml"));
    }

    private static void assertNoBadPackets(String configText) {
        assertFalse(configText.contains("bad-packets") || configText.contains("tick-end")
                || configText.contains("strike-interval"), configText);
    }

    @Test
    void templateRenderingQuotesStrings() {
        assertEquals("\"a\\\\b\\\"c\"", ConfigTemplate.render("a\\b\"c"));
        assertEquals("true", ConfigTemplate.render(true));
        assertEquals("0.8", ConfigTemplate.render(0.8));
    }
}
