// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.yaml.SnakeYamlParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DebugAndIntegrityTextsTest {

    private static final Pattern RAW_CODE = Pattern.compile("[&][0-9a-fk-orxA-FK-ORX#]");
    private static final List<String> NEW_KEYS = List.of("alert-bad-packets", "bad-packets-forged-teleport-confirm",
            "bad-packets-tick-end-spam", "alert-format-bad-packets", "debug-on", "debug-off", "debug-auto",
            "debug-status-line", "debug-trace-on", "debug-trace-off", "debug-player-offline");
    private static final List<String> BAD_PACKET_KEYS = List.of("alert-bad-packets", "alert-format-bad-packets",
            "bad-packets-forged-teleport-confirm", "bad-packets-tick-end-spam");
    private static final Pattern PRESET_KEY = Pattern.compile("^[A-Za-z_-]{1,40}$");

    @Test
    void everyLanguageCarriesTheSameKeysAsEnglish() throws Exception {
        Set<String> english = keysOf(LanguageId.EN);
        assertTrue(english.containsAll(NEW_KEYS));
        for (LanguageId language : LanguageId.values()) {
            assertEquals(english, keysOf(language), language.id());
        }
    }

    @Test
    void newKeysAreValidPresetOverrideKeys() {
        NEW_KEYS.forEach(key -> assertTrue(PRESET_KEY.matcher(key).matches(), key));
    }

    @Test
    void aPresetCanRewordTheDebugRepliesButNeverTheBadPacketTexts() {
        Map<String, Object> overrides = new java.util.LinkedHashMap<>();
        BAD_PACKET_KEYS.forEach(key -> overrides.put(key, "&cchanged"));
        overrides.put("debug-on", "&aON");
        Map<String, Object> preset = Map.of("messages", overrides);
        ConfigReference reference = new ConfigReference(new ConfigAssembler().assemble(TestConfigs.core("language: en"),
                ConfigTree.empty(), Optional.of(new PresetSnapshot(Optional.empty(), "h",
                        new PresetLayout().toEffective(preset)))).config());
        MessageBook book = TestConfigs.messages(reference);
        assertEquals("&aON", book.rawText("debug-on"));
        MessageBook bundled = bookIn(LanguageId.EN);
        for (String key : BAD_PACKET_KEYS) {
            assertEquals(bundled.rawText(key), book.rawText(key), key);
            assertTrue(reference.get().messages().overrides().text(key).isEmpty(), key);
        }
    }

    @Test
    void renderedDebugAndIntegrityTextsCarryNoRawColourCodes() {
        TemplateValues values = TemplateValues.of("{PLAYER}", "Steve").and("{LINE}", "Players online 1")
                .and("{MODEL}", "Bad packets").and("{BUFFER}", "16.1").and("{CHECK}", "x").and("{COUNT}", 3)
                .and("{WINDOW}", 60).and("{PROBABILITY}", "100%").and("{VL}", 0).and("{STEP_PERCENT}", 33);
        for (LanguageId language : LanguageId.values()) {
            MessageBook book = bookIn(language);
            for (String key : NEW_KEYS) {
                assertFalse(book.rawText(key).startsWith("&cNo text"), language + " " + key);
                String rendered = book.prefixed(key, values);
                assertFalse(RAW_CODE.matcher(rendered).find(), language + " " + key + ": " + rendered);
                String plain = book.strip(rendered);
                assertFalse(plain.contains("§") || RAW_CODE.matcher(plain).find(), language + " " + plain);
            }
        }
    }

    @Test
    void theIntegrityAlertNamesTheCheckInsteadOfAnAimProbability() {
        for (LanguageId language : LanguageId.values()) {
            String template = bookIn(language).rawText("alert-format-bad-packets");
            assertFalse(template.contains("Aim-Ai") || template.contains("{PROBABILITY}"), language + " " + template);
            assertTrue(template.contains("{MODEL}") && template.contains("{PLAYER}") && template.contains("{BUFFER}"),
                    language + " " + template);
        }
        MessageBook english = bookIn(LanguageId.EN);
        String line = english.strip(english.text("alert-format-bad-packets", TemplateValues.of("{MODEL}", "Bad packets")
                .and("{PLAYER}", "Forger").and("{BUFFER}", "16.1")));
        assertEquals("[Bad packets] Forger Failed check (Packet integrity) (Buffer: 16.1)", line);
    }

    private static MessageBook bookIn(LanguageId language) {
        return TestConfigs.messages(new ConfigReference(EffectiveConfig.pluginDefaults(
                TestConfigs.core("language: " + language.id()))));
    }

    private static Set<String> keysOf(LanguageId language) throws Exception {
        String text = new ClasspathResources(DebugAndIntegrityTextsTest.class.getClassLoader())
                .text("messages_" + language.id() + ".yml").orElseThrow();
        Map<String, Object> parsed = new SnakeYamlParser().parse(text);
        return parsed.keySet();
    }
}
