// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.BuiltInTexts;
import italy.mlsac.net.suppression.live.SuppressionMessages;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageDefaults;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuppressionMessagesTest {

    private static final List<String> KEYS = List.of("suppression-alert-flag", "suppression-alert-buffer",
            "suppression-shuffle-inventory", "suppression-drop-weapon", "suppression-launch");

    private final MessageDefaults defaults = SuppressionMessages.defaults();

    @Test
    void everyLanguageHasItsOwnUnbrandedText() {
        for (LanguageId language : LanguageId.values()) {
            for (String key : KEYS) {
                String text = defaults.text(key, language).orElseThrow();
                assertTrue(text.startsWith("&f{PLAYER} &7"), language + " " + key);
                assertFalse(text.contains("MLSAC") || text.contains("▍") || text.contains("&#747474C"),
                        language + " " + key);
                if (language != LanguageId.EN) {
                    assertNotEquals(defaults.text(key, LanguageId.EN), defaults.text(key, language),
                            language + " " + key);
                }
            }
        }
    }

    @Test
    void defaultPrefixPlusEnglishTextKeepsTheOldLook() {
        for (String key : KEYS) {
            assertEquals(BuiltInTexts.english().get(key), defaults.text(key, LanguageId.EN).orElseThrow(), key);
        }
    }

    @Test
    void placeholdersMatchAcrossLanguages() {
        for (LanguageId language : LanguageId.values()) {
            String flag = defaults.text("suppression-alert-flag", language).orElseThrow();
            for (String placeholder : List.of("{PLAYER}", "{VL}", "{STAGE}", "{STAGES}", "{ACTIONS}")) {
                assertTrue(flag.contains(placeholder), language + " " + placeholder);
            }
            String buffer = defaults.text("suppression-alert-buffer", language).orElseThrow();
            for (String placeholder : List.of("{PLAYER}", "{PERCENT}", "{BUFFER}", "{ACTIONS}")) {
                assertTrue(buffer.contains(placeholder), language + " " + placeholder);
            }
            for (String key : KEYS.subList(2, 5)) {
                assertTrue(defaults.text(key, language).orElseThrow().contains("{BUFFER}"), language + " " + key);
            }
        }
    }
}
