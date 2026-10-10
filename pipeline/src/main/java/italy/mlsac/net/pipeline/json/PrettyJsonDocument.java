// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

import java.util.ArrayList;
import java.util.List;

public final class PrettyJsonDocument {
    private static final String INDENT = "  ";
    private static final JsonEscaping ESCAPING = JsonEscaping.HTML_SAFE;

    private final List<String> members = new ArrayList<>();

    public PrettyJsonDocument text(String key, String value) {
        return member(key, ESCAPING.quote(value));
    }

    public PrettyJsonDocument integer(String key, long value) {
        return member(key, Long.toString(value));
    }

    public PrettyJsonDocument bool(String key, boolean value) {
        return member(key, Boolean.toString(value));
    }

    public PrettyJsonDocument textList(String key, List<String> values) {
        if (values.isEmpty()) {
            return member(key, "[]");
        }
        StringBuilder list = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            list.append(index == 0 ? "\n" : ",\n").append(INDENT).append(INDENT).append(ESCAPING.quote(values.get(index)));
        }
        list.append('\n').append(INDENT).append(']');
        return member(key, list.toString());
    }

    private PrettyJsonDocument member(String key, String valueText) {
        members.add(INDENT + ESCAPING.quote(key) + ": " + valueText);
        return this;
    }

    public String render() {
        if (members.isEmpty()) {
            return "{}";
        }
        return "{\n" + String.join(",\n", members) + "\n}";
    }
}
