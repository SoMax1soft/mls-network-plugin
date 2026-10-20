// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.modelchoice;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageDefaults;
import java.util.Locale;

public final class ModelChoiceTexts {

    static final String USAGE = "usage-model";
    static final String STATE = "model-state";
    static final String SET = "model-set";
    static final String FALLBACK = "model-fallback";
    static final String UNKNOWN = "model-unknown";
    static final String NOTHING_AVAILABLE = "model-none-available";

    private ModelChoiceTexts() {
    }

    static String nameKey(ModelChoice choice) {
        return "model-name-" + choice.id();
    }

    static String reasonKey(FallbackReason reason) {
        return "model-reason-" + reason.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static MessageDefaults defaults() {
        MessageDefaults.Builder builder = MessageDefaults.builder();
        english(builder);
        russian(builder);
        return builder.build();
    }

    private static void english(MessageDefaults.Builder builder) {
        LanguageId en = LanguageId.EN;
        builder.text(en, USAGE, "&7/mlsac model <player> [default|pro|mini|ultra]")
                .text(en, STATE, "&f{PLAYER}&7 is checked by &f{MODEL}&7. Available: &f{AVAILABLE}")
                .text(en, SET, "&f{PLAYER}&7 is now checked by &f{MODEL}")
                .text(en, FALLBACK, "&f{PLAYER}&7 asked for &f{REQUESTED}&7, which is not available ({REASON}); "
                        + "the server default is used")
                .text(en, UNKNOWN, "&cUnknown model &f{MODEL}&c. Use default, pro, mini or ultra")
                .text(en, NOTHING_AVAILABLE, "none")
                .text(en, nameKey(ModelChoice.DEFAULT), "server default")
                .text(en, nameKey(ModelChoice.PRO), "Pro")
                .text(en, nameKey(ModelChoice.MINI), "Mini")
                .text(en, nameKey(ModelChoice.ULTRA), "Ultra")
                .text(en, reasonKey(FallbackReason.DETECTION_OFF), "detection is off")
                .text(en, reasonKey(FallbackReason.DISABLED_IN_PRESET), "switched off in the preset")
                .text(en, reasonKey(FallbackReason.REFUSED_BY_BACKEND), "refused by the backend")
                .text(en, reasonKey(FallbackReason.NOT_SERVED_BY_BACKEND), "not served for this licence");
    }

    private static void russian(MessageDefaults.Builder builder) {
        LanguageId ru = LanguageId.RU;
        builder.text(ru, USAGE, "&7/mlsac model <игрок> [default|pro|mini|ultra]")
                .text(ru, STATE, "&f{PLAYER}&7 проверяет &f{MODEL}&7. Доступно: &f{AVAILABLE}")
                .text(ru, SET, "&f{PLAYER}&7 теперь проверяет &f{MODEL}")
                .text(ru, FALLBACK, "&f{PLAYER}&7: модель &f{REQUESTED}&7 недоступна ({REASON}), "
                        + "используется модель сервера")
                .text(ru, UNKNOWN, "&cНеизвестная модель &f{MODEL}&c. Варианты: default, pro, mini, ultra")
                .text(ru, NOTHING_AVAILABLE, "нет")
                .text(ru, nameKey(ModelChoice.DEFAULT), "модель сервера")
                .text(ru, nameKey(ModelChoice.PRO), "Pro")
                .text(ru, nameKey(ModelChoice.MINI), "Mini")
                .text(ru, nameKey(ModelChoice.ULTRA), "Ultra")
                .text(ru, reasonKey(FallbackReason.DETECTION_OFF), "детекция выключена")
                .text(ru, reasonKey(FallbackReason.DISABLED_IN_PRESET), "выключена в пресете")
                .text(ru, reasonKey(FallbackReason.REFUSED_BY_BACKEND), "бэкенд отказал")
                .text(ru, reasonKey(FallbackReason.NOT_SERVED_BY_BACKEND), "не выдаётся по этой лицензии");
    }
}
