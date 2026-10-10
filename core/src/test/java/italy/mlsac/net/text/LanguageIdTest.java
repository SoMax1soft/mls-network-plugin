// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LanguageIdTest {

    @ParameterizedTest
    @CsvSource({"zh-CN, ZH", "cn, ZH", "ua, UK", "en_US, EN", "de, EN", "RU, RU", "pt, EN", "' uk ', UK", "'', EN",
            "vi, VI", "ar, AR", "es-MX, ES"})
    void normalisesServerLanguage(String raw, LanguageId expected) {
        assertEquals(expected, LanguageId.normalise(raw));
    }
}
