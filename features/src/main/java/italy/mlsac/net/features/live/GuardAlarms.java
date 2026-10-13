// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.config.guard.GuardMode;
import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.guard.ConsoleOrder;
import italy.mlsac.net.guard.DisplayText;
import italy.mlsac.net.guard.GuardReason;
import italy.mlsac.net.guard.GuardRefusal;
import italy.mlsac.net.guard.GuardReport;
import italy.mlsac.net.guard.RepeatGate;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.MessageDefaults;
import italy.mlsac.net.text.TemplateValues;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class GuardAlarms implements GuardReport {

    private static final String TAG = "[CommandGuard] ";

    private final MessageBook messages;
    private final MessageDefaults texts;
    private final Consumer<String> staff;
    private final Logbook log;
    private final LongSupplier clock;
    private final RepeatGate consoleRepeats = new RepeatGate();
    private final RepeatGate staffRepeats = new RepeatGate();

    public GuardAlarms(MessageBook messages, MessageDefaults texts, Consumer<String> staff, Logbook log,
                       LongSupplier clock) {
        this.messages = messages;
        this.texts = texts;
        this.staff = staff;
        this.log = log;
        this.clock = clock;
    }

    @Override
    public void refused(ConsoleOrder order, GuardRefusal refusal, GuardMode mode) {
        long now = clock.getAsLong();
        String key = refusal.reason().id() + '|' + order.origin().kind().id() + '|' + rootOf(order.command());
        OptionalInt held = consoleRepeats.admit(key, now);
        if (held.isPresent()) {
            log.warn(TAG + (mode.dispatchesRefused() ? "Would block (mode warn, ran anyway) " : "Blocked ")
                    + "console command from " + order.origin().english() + ": " + DisplayText.command(order.command())
                    + " - " + refusal.english() + repeated(held.getAsInt()));
        }
        if (refusal.reason().alertsStaff() && staffRepeats.admit(key, now).isPresent()) {
            staff.accept(line(mode.dispatchesRefused() ? GuardMessages.WARNED : GuardMessages.BLOCKED,
                    TemplateValues.of("{ORIGIN}", origin(order.origin()))
                            .and("{REASON}", reason(refusal))
                            .and("{COMMAND}", DisplayText.command(order.command()))));
        }
    }

    @Override
    public void burst(String rootKey, int players, int pauseMinutes, GuardMode mode) {
        String root = DisplayText.shortened(rootKey);
        log.warn(TAG + players + " different players got '" + root + "' within a minute - "
                + (mode.dispatchesRefused() ? "would pause site commands (mode warn)"
                : "site commands paused for " + pauseMinutes + " min"));
        staff.accept(line(mode.dispatchesRefused() ? GuardMessages.PAUSE_WARNED : GuardMessages.PAUSED,
                TemplateValues.of("{COUNT}", players).and("{MINUTES}", pauseMinutes).and("{ROOT}", root)));
    }

    private String line(String key, TemplateValues values) {
        return messages.colorize(messages.rawPrefix()) + messages.colorize(values.apply(text(key)));
    }

    private String text(String key) {
        return texts.text(key, messages.messageLanguage()).orElse(key);
    }

    private String origin(CommandOrigin origin) {
        return text(GuardMessages.originKey(origin.kind()))
                .replace(CommandOrigin.LEVEL, String.valueOf(origin.level()));
    }

    private String reason(GuardRefusal refusal) {
        return text(GuardMessages.reasonKey(refusal.reason()))
                .replace(GuardReason.DETAIL, DisplayText.shortened(refusal.detail()));
    }

    private static String rootOf(String command) {
        String trimmed = command.trim();
        int space = trimmed.indexOf(' ');
        return DisplayText.shortened(space >= 0 ? trimmed.substring(0, space) : trimmed);
    }

    private static String repeated(int held) {
        return held > 0 ? " (+" + held + " similar in the last 5 min)" : "";
    }
}
