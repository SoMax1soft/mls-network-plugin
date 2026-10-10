// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import java.util.Set;

public final class StockTrickTexts {

    private static final String BRAND = "&#F1F1F1▍ &#D8D8D8M&#BFBFBFL&#A6A6A6S&#8D8D8DA&#747474C ";
    private static final String BUFFER_TAIL = " at &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e"
            + "&#CF0E3Br: &f{BUFFER}&#EE1955)&f";

    private final Set<String> texts;

    public StockTrickTexts(Set<String> texts) {
        this.texts = Set.copyOf(texts);
    }

    public static StockTrickTexts legacyEditorTexts() {
        return new StockTrickTexts(Set.of(
                BRAND + "&f{PLAYER} &7Forced to shuffle his inventory" + BUFFER_TAIL,
                BRAND + "&f{PLAYER} &7Forced to drop his weapon" + BUFFER_TAIL,
                "&c{PLAYER} &7was launched at buffer &f{BUFFER}&7.",
                "&cMLSAC shuffled {PLAYER}'s inventory at buffer {BUFFER}.",
                "&cMLSAC launched {PLAYER}'s weapon at buffer {BUFFER}."));
    }

    public boolean isStock(String text) {
        return texts.contains(text);
    }
}
