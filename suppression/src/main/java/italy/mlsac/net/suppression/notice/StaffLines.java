// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import italy.mlsac.net.suppression.plan.ConsoleEcho;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.server.SuppressionTexts;
import java.util.List;
import java.util.Optional;

public final class StaffLines {

    private final SuppressionTexts texts;
    private final SuppressionNotices notices;

    public StaffLines(SuppressionTexts texts, SuppressionNotices notices) {
        this.texts = texts;
        this.notices = notices;
    }

    public Optional<String> compose(StaffLineFormat format, List<String> tokens) {
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        String template = texts.presetOverride(format.key().key()).orElseGet(() -> texts.bundled(format.key().key()));
        if (template == null || template.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new StaffTemplate(texts.prefix(), template)
                .render(format.placeholders(String.join(", ", tokens))));
    }

    public void send(StaffLineFormat format, List<String> tokens, ConsoleEcho echo) {
        compose(format, tokens).ifPresent(line -> {
            notices.toAlertSubscribers(line);
            if (echo == ConsoleEcho.ECHO_STAFF_LINES) {
                notices.toConsole(line);
            }
        });
    }
}
