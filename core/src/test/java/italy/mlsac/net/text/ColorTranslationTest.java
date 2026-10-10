// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class ColorTranslationTest {

    private final HexColorNormalizer normalizer = new HexColorNormalizer();
    private final LegacyColorizer colorizer = new LegacyColorizer(normalizer);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "&x&F&F&5&5&5&5Hi|&#FF5555Hi",
            "§x§f§f§5§5§5§5Hi|&#ff5555Hi",
            "<#FF5555>Hi</#FF5555>|&#FF5555Hi",
            "<color:#00ff00>ok</color>|&#00ff00ok",
            "{#123456}x|&#123456x",
            "&#F55x|&#FF5555x",
            "&#FF5555|&#FF5555",
            "#FF5555 text|&#FF5555 text",
            "a #FF5555|a &#FF5555",
            "a1#FF5555|a1#FF5555",
            "#FF55551|#FF55551",
            "§#abcdef!|&#abcdef!",
            "§#ABC|&#AABBCC",
            "plain &atext|plain &atext"
    })
    void normalisesEveryHexSpelling(String input, String expected) {
        assertEquals(expected, normalizer.normalise(input));
    }

    @Test
    void translatesToSectionCodes() {
        assertEquals("§x§F§F§5§5§5§5Hi §cred §lbold", colorizer.colorize("&#FF5555Hi &cred &lbold"));
        assertEquals("keep & alone &z", colorizer.colorize("keep & alone &z"));
        assertEquals("§x§f§f§5§5§5§5x", colorizer.colorize("&x&f&f&5&5&5&5x"));
    }

    @Test
    void stripsForConsole() {
        assertEquals("[Ultra] Suspect 95%", colorizer.strip("&#81FFC2[Ultra] &fSuspect &c95%"));
        assertEquals("Hi", colorizer.strip("§x§F§F§5§5§5§5Hi"));
    }
}
