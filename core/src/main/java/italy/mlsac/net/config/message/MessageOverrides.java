// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record MessageOverrides(Map<String, MessageOverride> entries) {

    public MessageOverrides {
        entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public static MessageOverrides none() {
        return new MessageOverrides(Map.of());
    }

    public Optional<String> text(String key) {
        MessageOverride entry = entries.get(key);
        return entry instanceof MessageOverride.Text text ? Optional.of(text.value()) : Optional.empty();
    }

    public Optional<List<String>> lines(String key) {
        MessageOverride entry = entries.get(key);
        return entry instanceof MessageOverride.Lines lines ? Optional.of(lines.values()) : Optional.empty();
    }
}
