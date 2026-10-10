// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.messages;

import italy.mlsac.net.text.LanguageId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BundledTexts {

    private final Map<LanguageId, Map<String, Object>> files;

    public BundledTexts(Map<LanguageId, Map<String, Object>> files) {
        Map<LanguageId, Map<String, Object>> copy = new EnumMap<>(LanguageId.class);
        files.forEach((language, entries) -> copy.put(language, Collections.unmodifiableMap(entries)));
        this.files = Collections.unmodifiableMap(copy);
    }

    public static BundledTexts empty() {
        return new BundledTexts(Map.of());
    }

    public Optional<String> text(LanguageId language, String key) {
        Object value = files.getOrDefault(language, Map.of()).get(key);
        return value instanceof String text ? Optional.of(text) : Optional.empty();
    }

    public Optional<List<String>> lines(LanguageId language, String key) {
        return node(language, key).flatMap(BundledTexts::asLines);
    }

    public Optional<Object> node(LanguageId language, String dottedPath) {
        Object current = files.getOrDefault(language, Map.of());
        for (String part : dottedPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return Optional.empty();
            }
            current = map.get(part);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    public Optional<String> nodeText(LanguageId language, String dottedPath) {
        return node(language, dottedPath).filter(String.class::isInstance).map(String.class::cast);
    }

    private static Optional<List<String>> asLines(Object value) {
        if (!(value instanceof List<?> list)) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>(list.size());
        list.forEach(line -> lines.add(String.valueOf(line)));
        return Optional.of(List.copyOf(lines));
    }
}
