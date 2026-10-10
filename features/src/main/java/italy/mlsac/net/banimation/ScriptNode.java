// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ScriptNode {

    private final Map<?, ?> values;

    ScriptNode(Map<?, ?> values) {
        this.values = values;
    }

    int integer(String key, int fallback) {
        return values.get(key) instanceof Number number ? number.intValue() : fallback;
    }

    double decimal(String key, double fallback) {
        return values.get(key) instanceof Number number ? number.doubleValue() : fallback;
    }

    float single(String key, float fallback) {
        return values.get(key) instanceof Number number ? number.floatValue() : fallback;
    }

    boolean flag(String key, boolean fallback) {
        return values.get(key) instanceof Boolean value ? value : fallback;
    }

    String text(String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    boolean has(String key) {
        return values.containsKey(key);
    }

    Optional<ScriptNode> child(String key) {
        return values.get(key) instanceof Map<?, ?> map ? Optional.of(new ScriptNode(map)) : Optional.empty();
    }

    List<ScriptNode> children(String key) {
        List<ScriptNode> nodes = new ArrayList<>();
        if (values.get(key) instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    nodes.add(new ScriptNode(map));
                }
            }
        }
        return nodes;
    }

    List<Integer> integers(String key) {
        List<Integer> numbers = new ArrayList<>();
        if (values.get(key) instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Number number) {
                    numbers.add(number.intValue());
                }
            }
        }
        return numbers;
    }
}
