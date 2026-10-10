// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConfigTree {

    private static final ConfigTree EMPTY = new ConfigTree(Map.of());

    private final Map<String, Object> root;

    private ConfigTree(Map<String, Object> root) {
        this.root = root;
    }

    public static ConfigTree empty() {
        return EMPTY;
    }

    public static ConfigTree of(Map<?, ?> source) {
        return source == null ? EMPTY : new ConfigTree(copyMap(source));
    }

    public Optional<Object> value(String path) {
        Object current = root;
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(key)) {
                return Optional.empty();
            }
            current = map.get(key);
        }
        return Optional.ofNullable(current);
    }

    public boolean contains(String path) {
        return value(path).isPresent();
    }

    public Optional<ConfigTree> section(String path) {
        return value(path).filter(Map.class::isInstance).map(v -> new ConfigTree(castMap(v)));
    }

    public Set<String> keys() {
        return root.keySet();
    }

    public Map<String, Object> asMap() {
        return root;
    }

    public boolean isEmpty() {
        return root.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(String.valueOf(key), copyValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(element -> copy.add(copyValue(element)));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
