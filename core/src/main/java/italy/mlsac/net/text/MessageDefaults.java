// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MessageDefaults {

    private static final MessageDefaults NONE = new MessageDefaults(Map.of());

    private final Map<LanguageId, Map<String, String>> texts;

    private MessageDefaults(Map<LanguageId, Map<String, String>> texts) {
        this.texts = texts;
    }

    public static MessageDefaults none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> text(String key, LanguageId language) {
        String translated = texts.getOrDefault(language, Map.of()).get(key);
        if (translated != null) {
            return Optional.of(translated);
        }
        return Optional.ofNullable(texts.getOrDefault(LanguageId.EN, Map.of()).get(key));
    }

    public static final class Builder {

        private final Map<LanguageId, Map<String, String>> texts = new EnumMap<>(LanguageId.class);

        public Builder text(LanguageId language, String key, String value) {
            texts.computeIfAbsent(language, l -> new LinkedHashMap<>()).put(key, value);
            return this;
        }

        public MessageDefaults build() {
            Map<LanguageId, Map<String, String>> copy = new EnumMap<>(LanguageId.class);
            texts.forEach((language, map) -> copy.put(language, Collections.unmodifiableMap(new LinkedHashMap<>(map))));
            return new MessageDefaults(Collections.unmodifiableMap(copy));
        }
    }
}
