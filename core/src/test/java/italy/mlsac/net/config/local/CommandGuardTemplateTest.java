// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.local;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.MemoryDataFolder;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.yaml.SnakeYamlParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandGuardTemplateTest {

    private final MemoryDataFolder folder = new MemoryDataFolder();
    private final ConsoleLog log = TestLogs.capturing(new CapturingLogSink(), new ManualClock(0), false);
    private final LocalConfigFile file = new LocalConfigFile(folder,
            new ClasspathResources(getClass().getClassLoader()), new SnakeYamlParser(), log);

    @Test
    void missingGuardKeysAreAddedWithoutLosingTheOwnersRoots() throws Exception {
        folder.writeText("config.yml", "_schema-version: '2.0'\nlanguage: en\ndetection:\n  api-key: k-1\n"
                + "command-guard:\n  mode: warn\n  extra-allowed-roots:\n    - cmi\n    - \"essentials:mail\"\n");
        ConfigTree tree = file.prepare();
        assertEquals("warn", tree.value("command-guard.mode").orElseThrow());
        assertEquals(List.of("cmi", "essentials:mail"), tree.value("command-guard.extra-allowed-roots").orElseThrow());
        assertEquals(true, tree.value("command-guard.enabled").orElseThrow());
        assertEquals(15, tree.value("command-guard.max-same-command-burst").orElseThrow());
        assertEquals("k-1", tree.value("detection.api-key").orElseThrow());
    }

    @Test
    void anOldConfigGetsTheGuardSection() throws Exception {
        folder.writeText("config.yml", "_schema-version: '2.0'\nlanguage: en\n");
        ConfigTree tree = file.prepare();
        assertEquals("block", tree.value("command-guard.mode").orElseThrow());
        assertEquals(List.of(), tree.value("command-guard.extra-allowed-roots").orElseThrow());
    }

    @Test
    void listsRenderAsFlowSequences() {
        assertEquals("[\"cmi\", \"a\\\"b\"]", ConfigTemplate.render(List.of("cmi", "a\"b")));
        assertEquals("[]", ConfigTemplate.render(List.of()));
    }
}
