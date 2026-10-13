// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageDefaults;

public final class SuppressionMessages {

    private static final String BUFFER_TAIL = " &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e"
            + "&#CF0E3Br: &f{BUFFER}&#EE1955)&f";
    private static final String STOCK_BUFFER_TAIL = " at" + BUFFER_TAIL;

    private SuppressionMessages() {
    }

    public static MessageDefaults defaults() {
        MessageDefaults.Builder builder = MessageDefaults.builder();
        add(builder, LanguageId.EN, new Wording("Suppression", "Flag", "stage", "Buffer",
                "Forced to shuffle his inventory" + STOCK_BUFFER_TAIL, "Forced to drop his weapon" + STOCK_BUFFER_TAIL,
                "Was launched" + BUFFER_TAIL));
        add(builder, LanguageId.RU, new Wording("Подавление", "Флаг", "этап", "Буфер",
                "Инвентарь перемешан" + BUFFER_TAIL, "Выбито оружие" + BUFFER_TAIL,
                "Подброшен в воздух" + BUFFER_TAIL));
        add(builder, LanguageId.UK, new Wording("Придушення", "Флаг", "етап", "Буфер",
                "Інвентар перемішано" + BUFFER_TAIL, "Вибито зброю" + BUFFER_TAIL,
                "Підкинуто в повітря" + BUFFER_TAIL));
        add(builder, LanguageId.ES, new Wording("Supresión", "Flag", "etapa", "Buffer",
                "Inventario mezclado" + BUFFER_TAIL, "Arma derribada" + BUFFER_TAIL,
                "Lanzado por los aires" + BUFFER_TAIL));
        add(builder, LanguageId.VI, new Wording("Áp chế", "Cờ", "giai đoạn", "Buffer",
                "Bị xáo trộn túi đồ" + BUFFER_TAIL, "Bị đánh rơi vũ khí" + BUFFER_TAIL,
                "Bị hất lên không trung" + BUFFER_TAIL));
        add(builder, LanguageId.ZH, new Wording("压制", "标记", "阶段", "缓冲",
                "背包已被打乱" + BUFFER_TAIL, "武器已被打落" + BUFFER_TAIL,
                "已被抛向空中" + BUFFER_TAIL));
        add(builder, LanguageId.AR, new Wording("كبح", "إنذار", "المرحلة", "البافر",
                "تم خلط المخزون" + BUFFER_TAIL, "تم إسقاط سلاحه" + BUFFER_TAIL,
                "تم قذفه في الهواء" + BUFFER_TAIL));
        return builder.build();
    }

    private static void add(MessageDefaults.Builder builder, LanguageId language, Wording wording) {
        builder.text(language, "suppression-alert-flag", "&f{PLAYER} &7" + wording.suppression()
                        + " &#E5396A(&#E5396A" + wording.flag() + " &f{VL}&#E5396A, " + wording.stage()
                        + " &f{STAGE}/{STAGES}&#E5396A)&7: &f{ACTIONS}")
                .text(language, "suppression-alert-buffer", "&f{PLAYER} &7" + wording.suppression()
                        + " &#E5396A(&#E5396A" + wording.buffer() + " ≥ &f{PERCENT}%&#E5396A, &f{BUFFER}&#E5396A)&7: "
                        + "&f{ACTIONS}")
                .text(language, "suppression-shuffle-inventory", "&f{PLAYER} &7" + wording.shuffle())
                .text(language, "suppression-drop-weapon", "&f{PLAYER} &7" + wording.drop())
                .text(language, "suppression-launch", "&f{PLAYER} &7" + wording.launch());
    }

    private record Wording(String suppression, String flag, String stage, String buffer, String shuffle,
                           String drop, String launch) {
    }
}
