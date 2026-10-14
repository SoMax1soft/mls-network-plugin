// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.message;

import italy.mlsac.net.config.CleanText;
import italy.mlsac.net.config.SettingsLayer;
import italy.mlsac.net.text.LanguageId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class MessageReader {

    private static final Pattern KEY = Pattern.compile("^[A-Za-z_-]{1,40}$");
    private static final int MAX_KEYS = 150;
    private static final int MAX_TEXT = 512;
    private static final int MAX_LINES = 10;
    private static final int MAX_LINE = 96;

    private final LocalOnlyMessages localOnly = new LocalOnlyMessages();

    public MessageSettings read(MessageSettings lower, SettingsLayer layer) {
        MessageOverrides overrides = readOverrides(layer).orElse(lower.overrides());
        Optional<LanguageId> language = lower.presetLanguage();
        Optional<String> requested = layer.text("messages-language", 8);
        if (requested.isPresent()) {
            Optional<LanguageId> parsed = LanguageId.exact(requested.get());
            if (parsed.isPresent()) {
                language = parsed;
            } else {
                layer.drop("messages-language", "unsupported language");
            }
        }
        return new MessageSettings(overrides, language);
    }

    private Optional<MessageOverrides> readOverrides(SettingsLayer layer) {
        Optional<SettingsLayer> section = layer.section("message-overrides");
        if (section.isEmpty()) {
            return Optional.empty();
        }
        Map<String, MessageOverride> entries = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : section.get().tree().asMap().entrySet()) {
            if (entries.size() >= MAX_KEYS) {
                break;
            }
            if (!KEY.matcher(entry.getKey()).matches()) {
                section.get().drop(entry.getKey(), "invalid message key");
                continue;
            }
            if (localOnly.contains(entry.getKey())) {
                section.get().drop(entry.getKey(), "local-only message, a preset cannot change it");
                continue;
            }
            toOverride(entry.getValue()).ifPresentOrElse(value -> {
                entries.put(entry.getKey(), value);
                section.get().accepted();
            }, () -> section.get().drop(entry.getKey(), "not a string or list"));
        }
        return Optional.of(new MessageOverrides(entries));
    }

    private static Optional<MessageOverride> toOverride(Object value) {
        if (value instanceof String text) {
            return Optional.of(new MessageOverride.Text(CleanText.of(text, MAX_TEXT)));
        }
        if (value instanceof List<?> list) {
            List<String> lines = new ArrayList<>();
            for (Object raw : list) {
                if (lines.size() >= MAX_LINES) {
                    break;
                }
                if (raw instanceof String line) {
                    lines.add(CleanText.of(line, MAX_LINE));
                }
            }
            return Optional.of(new MessageOverride.Lines(lines));
        }
        return Optional.empty();
    }
}
