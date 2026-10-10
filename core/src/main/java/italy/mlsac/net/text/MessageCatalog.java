// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import italy.mlsac.net.resource.ResourceSource;
import italy.mlsac.net.yaml.YamlFormatException;
import italy.mlsac.net.yaml.YamlParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MessageCatalog {

    private final Map<LanguageId, Map<String, Object>> languages;

    private MessageCatalog(Map<LanguageId, Map<String, Object>> languages) {
        this.languages = languages;
    }

    public static MessageCatalog load(ResourceSource resources, YamlParser yaml) throws YamlFormatException {
        Map<LanguageId, Map<String, Object>> loaded = new EnumMap<>(LanguageId.class);
        for (LanguageId language : LanguageId.values()) {
            Optional<String> text = resources.text("messages_" + language.id() + ".yml");
            if (text.isPresent()) {
                loaded.put(language, Collections.unmodifiableMap(yaml.parse(text.get())));
            }
        }
        return new MessageCatalog(Collections.unmodifiableMap(loaded));
    }

    public Optional<String> text(LanguageId language, String key) {
        Object value = languages.getOrDefault(language, Map.of()).get(key);
        return value instanceof String text ? Optional.of(text) : Optional.empty();
    }

    public Optional<List<String>> lines(LanguageId language, String key) {
        Object value = languages.getOrDefault(language, Map.of()).get(key);
        if (!(value instanceof List<?> list)) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>();
        list.forEach(line -> lines.add(String.valueOf(line)));
        return Optional.of(List.copyOf(lines));
    }

    public boolean hasLanguage(LanguageId language) {
        return languages.containsKey(language);
    }
}
