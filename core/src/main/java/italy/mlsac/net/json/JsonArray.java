// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.util.ArrayList;
import java.util.List;

public record JsonArray(List<JsonValue> elements) implements JsonValue {

    public JsonArray {
        elements = List.copyOf(elements);
    }

    public int size() {
        return elements.size();
    }

    public JsonValue get(int index) {
        return elements.get(index);
    }

    @Override
    public Object toPlain() {
        List<Object> plain = new ArrayList<>(elements.size());
        for (JsonValue element : elements) {
            plain.add(element.toPlain());
        }
        return plain;
    }
}
