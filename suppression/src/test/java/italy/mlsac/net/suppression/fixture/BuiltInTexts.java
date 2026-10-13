// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import java.util.Map;

public final class BuiltInTexts {

    public static final String PREFIX = "&#F1F1F1▍ &#D8D8D8M&#BFBFBFL&#A6A6A6S&#8D8D8DA&#747474C &r";
    public static final String BUFFER_TAIL = " &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e"
            + "&#CF0E3Br: &f{BUFFER}&#EE1955)&f";
    public static final String SHUFFLE = "&f{PLAYER} &7Forced to shuffle his inventory at" + BUFFER_TAIL;
    public static final String DROP = "&f{PLAYER} &7Forced to drop his weapon at" + BUFFER_TAIL;
    public static final String LAUNCH = "&f{PLAYER} &7Was launched" + BUFFER_TAIL;
    public static final String ALERT_FLAG = "&f{PLAYER} &7Suppression &#E5396A(&#E5396AFlag &f{VL}&#E5396A, "
            + "stage &f{STAGE}/{STAGES}&#E5396A)&7: &f{ACTIONS}";
    public static final String ALERT_BUFFER = "&f{PLAYER} &7Suppression &#E5396A(&#E5396ABuffer ≥ "
            + "&f{PERCENT}%&#E5396A, &f{BUFFER}&#E5396A)&7: &f{ACTIONS}";

    private BuiltInTexts() {
    }

    public static Map<String, String> english() {
        return Map.of(
                "suppression-shuffle-inventory", SHUFFLE,
                "suppression-drop-weapon", DROP,
                "suppression-launch", LAUNCH,
                "suppression-alert-flag", ALERT_FLAG,
                "suppression-alert-buffer", ALERT_BUFFER);
    }
}
