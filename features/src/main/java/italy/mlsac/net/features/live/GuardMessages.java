// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.guard.GuardReason;
import italy.mlsac.net.guard.OriginKind;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageDefaults;
import java.util.Map;

public final class GuardMessages {

    public static final String BLOCKED = "command-guard-blocked";
    public static final String WARNED = "command-guard-warned";
    public static final String PAUSED = "command-guard-paused";
    public static final String PAUSE_WARNED = "command-guard-paused-warn";
    private static final String REASON_PREFIX = "command-guard-reason-";
    private static final String ORIGIN_PREFIX = "command-guard-origin-";

    private static final Map<String, String> ENGLISH = Map.ofEntries(
            Map.entry(BLOCKED, "&cBlocked a site command &7({ORIGIN})&c: &f{COMMAND} &7- {REASON}"),
            Map.entry(WARNED, "&eA site command would be blocked &7({ORIGIN})&e, ran anyway (mode: warn): "
                    + "&f{COMMAND} &7- {REASON}"),
            Map.entry(PAUSED, "&c'{ROOT}' went to &f{COUNT}&c players within a minute - site commands are paused "
                    + "for &f{MINUTES}&c min. Check your preset and dashboard account on mlsac.net."),
            Map.entry(PAUSE_WARNED, "&e'{ROOT}' went to &f{COUNT}&e players within a minute - site commands would "
                    + "be paused (mode: warn)."));

    private static final Map<String, String> RUSSIAN = Map.ofEntries(
            Map.entry(BLOCKED, "&cЗаблокирована команда с сайта &7({ORIGIN})&c: &f{COMMAND} &7- {REASON}"),
            Map.entry(WARNED, "&eКоманда с сайта была бы заблокирована &7({ORIGIN})&e, но выполнена (режим warn): "
                    + "&f{COMMAND} &7- {REASON}"),
            Map.entry(PAUSED, "&c'{ROOT}' отправлена на &f{COUNT}&c игроков за минуту - команды с сайта "
                    + "приостановлены на &f{MINUTES}&c мин. Проверьте пресет и аккаунт на mlsac.net."),
            Map.entry(PAUSE_WARNED, "&e'{ROOT}' отправлена на &f{COUNT}&e игроков за минуту - команды с сайта "
                    + "были бы приостановлены (режим warn)."));

    private static final Map<GuardReason, String> RUSSIAN_REASONS = Map.ofEntries(
            Map.entry(GuardReason.CONTROL_CHARACTER, "перенос строки или управляющий символ"),
            Map.entry(GuardReason.TOO_LONG, "длиннее 256 символов"),
            Map.entry(GuardReason.CHAINING, "цепочка команд (; && || |)"),
            Map.entry(GuardReason.SELECTOR, "селектор {DETAIL}"),
            Map.entry(GuardReason.WORLDEDIT, "команда WorldEdit"),
            Map.entry(GuardReason.DENIED_ROOT, "'{DETAIL}' запрещена всегда"),
            Map.entry(GuardReason.DENIED_OWNER, "команда плагина {DETAIL}, запрещена всегда"),
            Map.entry(GuardReason.NOT_ALLOWED, "'{DETAIL}' не разрешена (command-guard.extra-allowed-roots "
                    + "в config.yml)"),
            Map.entry(GuardReason.UNKNOWN_COMMAND, "'{DETAIL}' - такой команды нет на сервере"),
            Map.entry(GuardReason.WRONG_TARGET, "цель не наказываемый игрок ({DETAIL})"),
            Map.entry(GuardReason.UNSAFE_NAME, "ник {DETAIL} не обычный ник Minecraft"),
            Map.entry(GuardReason.RATE_LIMIT, "больше {DETAIL} команд с сайта в минуту"),
            Map.entry(GuardReason.PLAYER_RATE_LIMIT, "больше {DETAIL} команд с сайта в минуту на одного игрока"),
            Map.entry(GuardReason.BURST, "одна команда ушла на слишком многих игроков"),
            Map.entry(GuardReason.PAUSED, "команды с сайта приостановлены после всплеска"));

    private static final Map<OriginKind, String> RUSSIAN_ORIGINS = Map.ofEntries(
            Map.entry(OriginKind.LADDER, "ступень наказаний VL {VL}"),
            Map.entry(OriginKind.LADDER_ANIMATION, "ступень наказаний VL {VL}, анимация бана"),
            Map.entry(OriginKind.PUNISH, "/mlsac punish"),
            Map.entry(OriginKind.PUNISH_ANIMATION, "/mlsac punish, анимация бана"),
            Map.entry(OriginKind.SUPPRESSION, "этап подавления VL {VL}"),
            Map.entry(OriginKind.SUPPRESSION_ANIMATION, "этап подавления VL {VL}, анимация бана"),
            Map.entry(OriginKind.MENU_BUTTON, "кнопка меню"));

    private GuardMessages() {
    }

    public static String reasonKey(GuardReason reason) {
        return REASON_PREFIX + reason.id();
    }

    public static String originKey(OriginKind kind) {
        return ORIGIN_PREFIX + kind.id();
    }

    public static MessageDefaults defaults() {
        MessageDefaults.Builder builder = MessageDefaults.builder();
        ENGLISH.forEach((key, text) -> builder.text(LanguageId.EN, key, text));
        RUSSIAN.forEach((key, text) -> builder.text(LanguageId.RU, key, text));
        for (GuardReason reason : GuardReason.values()) {
            builder.text(LanguageId.EN, reasonKey(reason), reason.english());
            builder.text(LanguageId.RU, reasonKey(reason), RUSSIAN_REASONS.get(reason));
        }
        for (OriginKind kind : OriginKind.values()) {
            builder.text(LanguageId.EN, originKey(kind), kind.english());
            builder.text(LanguageId.RU, originKey(kind), RUSSIAN_ORIGINS.get(kind));
        }
        return builder.build();
    }
}
