// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.response;

public final class DefaultTrickTexts {

    private static final String BRAND = "&#F1F1F1▍ &#D8D8D8M&#BFBFBFL&#A6A6A6S&#8D8D8DA&#747474C ";
    private static final String BUFFER_TAIL = " at &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e"
            + "&#CF0E3Br: &f{BUFFER}&#EE1955)&f";

    public static final String SHUFFLE = BRAND + "&f{PLAYER} &7Forced to shuffle his inventory" + BUFFER_TAIL;
    public static final String DROP = BRAND + "&f{PLAYER} &7Forced to drop his weapon" + BUFFER_TAIL;
    public static final String LAUNCH = "&c{PLAYER} &7was launched at buffer &f{BUFFER}&7.";

    private DefaultTrickTexts() {
    }
}
