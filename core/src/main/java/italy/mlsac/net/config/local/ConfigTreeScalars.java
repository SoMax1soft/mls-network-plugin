// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.local;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigTreeScalars {

    private ConfigTreeScalars() {
    }

    static boolean isScalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    static boolean isTemplateValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().allMatch(ConfigTreeScalars::isScalar);
        }
        return isScalar(value);
    }

    static Object mutableCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), mutableCopy(v)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(element -> copy.add(mutableCopy(element)));
            return copy;
        }
        return value;
    }
}
