// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.player.GrantNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class PluginDescriptorTest {

    private static final Set<SubcommandName> INTERNAL_VERBS = Set.of(SubcommandName.START, SubcommandName.STOP);

    @Test
    @SuppressWarnings("unchecked")
    void descriptorIsStrictYamlWithEveryNode() throws IOException {
        String text;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in);
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        LoaderOptions strict = new LoaderOptions();
        strict.setAllowDuplicateKeys(false);
        Map<String, Object> descriptor = new Yaml(new SafeConstructor(strict)).load(text);
        assertEquals("MLSAC", descriptor.get("name"));
        assertEquals("1.16", descriptor.get("api-version"));
        assertEquals(true, descriptor.get("folia-supported"));
        assertFalse(String.valueOf(descriptor.get("version")).contains("$"));
        Map<String, Object> command = (Map<String, Object>) ((Map<String, Object>) descriptor.get("commands")).get("mlsac");
        assertFalse(command.containsKey("permission"));
        Map<String, Object> permissions = (Map<String, Object>) descriptor.get("permissions");
        List<GrantNode> declared = new ArrayList<>(GrantNode.standalone());
        boolean publicEdition = "public".equals(System.getProperty("mlsac.edition"));
        for (SubcommandName subcommand : SubcommandName.values()) {
            if (!publicEdition || !INTERNAL_VERBS.contains(subcommand)) {
                declared.addAll(subcommand.grants());
            }
        }
        for (GrantNode grant : declared) {
            assertTrue(permissions.containsKey(grant.node()), grant.node());
        }
        assertFalse(text.contains("antiesp"));
        assertFalse(text.contains("vision"));
        assertFalse(text.contains("report"));
        if (publicEdition) {
            assertFalse(text.contains("collect"), "collect");
            assertFalse(text.contains("datastatus"), "datastatus");
            assertFalse(text.contains("start"), "start");
        } else {
            assertTrue(permissions.containsKey("mlsac.command.collect"));
        }
    }
}
