// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageDefaults;

public final class SuppressionMessages {

    private static final String BRAND = "&#F1F1F1▍ &#D8D8D8M&#BFBFBFL&#A6A6A6S&#8D8D8DA&#747474C &f{PLAYER} ";
    private static final String BUFFER_TAIL = " &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e"
            + "&#CF0E3Br: &f{BUFFER}&#EE1955)&f";
    private static final String STOCK_BUFFER_TAIL = " at &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e"
            + "&#CF0E3Br: &f{BUFFER}&#EE1955)&f";

    private SuppressionMessages() {
    }

    public static MessageDefaults defaults() {
        return MessageDefaults.builder()
                .text(LanguageId.EN, "suppression-alert-flag", BRAND + "&7Suppression &#E5396A(&#E5396AFlag &f{VL}"
                        + "&#E5396A, stage &f{STAGE}/{STAGES}&#E5396A)&7: &f{ACTIONS}")
                .text(LanguageId.RU, "suppression-alert-flag", BRAND + "&7Подавление &#E5396A(&#E5396AФлаг &f{VL}"
                        + "&#E5396A, этап &f{STAGE}/{STAGES}&#E5396A)&7: &f{ACTIONS}")
                .text(LanguageId.EN, "suppression-alert-buffer", BRAND + "&7Suppression &#E5396A(&#E5396ABuffer ≥ "
                        + "&f{PERCENT}%&#E5396A, &f{BUFFER}&#E5396A)&7: &f{ACTIONS}")
                .text(LanguageId.RU, "suppression-alert-buffer", BRAND + "&7Подавление &#E5396A(&#E5396AБуфер ≥ "
                        + "&f{PERCENT}%&#E5396A, &f{BUFFER}&#E5396A)&7: &f{ACTIONS}")
                .text(LanguageId.EN, "suppression-shuffle-inventory", BRAND + "&7Forced to shuffle his inventory"
                        + STOCK_BUFFER_TAIL)
                .text(LanguageId.RU, "suppression-shuffle-inventory", BRAND + "&7Инвентарь перемешан" + BUFFER_TAIL)
                .text(LanguageId.EN, "suppression-drop-weapon", BRAND + "&7Forced to drop his weapon"
                        + STOCK_BUFFER_TAIL)
                .text(LanguageId.RU, "suppression-drop-weapon", BRAND + "&7Выбито оружие" + BUFFER_TAIL)
                .text(LanguageId.EN, "suppression-launch", BRAND + "&7Was launched" + BUFFER_TAIL)
                .text(LanguageId.RU, "suppression-launch", BRAND + "&7Подброшен в воздух" + BUFFER_TAIL)
                .build();
    }
}
