// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.testkit.TestConfigs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MessageBookTest {

    private final ConfigReference reference = new ConfigReference(TestConfigs.defaults());
    private final MessageBook book = TestConfigs.messages(reference);

    @Test
    void serverLanguageDefaultsToRussianBundle() {
        assertEquals(LanguageId.RU, book.messageLanguage());
        assertTrue(book.rawText("alerts-enabled").length() > 0);
    }

    @Test
    void englishTextsComeFromTheBundledFile() {
        use(TestConfigs.core("language: en\n"), Map.of());
        assertEquals("&fAlerts &aenabled", book.rawText("alerts-enabled"));
        assertEquals("§fAlerts §aenabled", book.text("alerts-enabled", TemplateValues.none()));
        assertEquals(List.of("&7Sens: &f{SENS}%", "&7Client: &f{CLIENT}", "&7Detections (>80%): &f{DETECTIONS}"),
                book.rawLines("profile-info"));
    }

    @Test
    void presetOverridesAndLanguageWin() {
        use(TestConfigs.core("language: ru\n"), Map.of(
                "messages", Map.of("prefix", "&6[AC] &r", "alerts-enabled", "&aON", "profile-info", "just text"),
                "messages-language", "en"));
        assertEquals(LanguageId.EN, book.messageLanguage());
        assertEquals("§6[AC] §r§aON", book.prefixed("alerts-enabled", TemplateValues.none()));
        assertEquals(3, book.rawLines("profile-info").size());
        assertEquals("&fAlerts &cdisabled", book.rawText("alerts-disabled"));
    }

    @Test
    void placeholdersAndMissingKeys() {
        use(TestConfigs.core("language: en\n"), Map.of());
        assertEquals("§cPlayer not found: §fSteve",
                book.text("player-not-found", TemplateValues.of("{PLAYER}", "Steve")));
        assertEquals("&cNo text for 'no-such-key'", book.rawText("no-such-key"));
    }

    @Test
    void moduleDefaultsFillKeysTheBundleLacks() {
        use(TestConfigs.core("language: ru\n"), Map.of());
        book.registerDefaults(MessageDefaults.builder()
                .text(LanguageId.EN, "suppression-alert-flag", "english")
                .text(LanguageId.RU, "suppression-alert-flag", "russian")
                .text(LanguageId.EN, "suppression-launch", "launch-en")
                .build());
        assertEquals("russian", book.rawText("suppression-alert-flag"));
        assertEquals("launch-en", book.rawText("suppression-launch"));
    }

    private void use(CoreSettings core, Map<String, Object> presetConfig) {
        Optional<PresetSnapshot> snapshot = presetConfig.isEmpty() ? Optional.empty()
                : Optional.of(new PresetSnapshot(Optional.of("t"), "h", new PresetLayout().toEffective(presetConfig)));
        EffectiveConfig config = new ConfigAssembler().assemble(core, ConfigTree.empty(), snapshot).config();
        reference.publish(config);
    }
}
