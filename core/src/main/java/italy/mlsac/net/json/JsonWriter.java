// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class JsonWriter {

    private final JsonLayout layout;
    private final JsonEscaping escaping;

    public JsonWriter(JsonLayout layout, JsonEscaping escaping) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.escaping = Objects.requireNonNull(escaping, "escaping");
    }

    public static JsonWriter compact() {
        return new JsonWriter(JsonLayout.COMPACT, JsonEscaping.STANDARD);
    }

    public String write(JsonValue value) {
        StringBuilder out = new StringBuilder(256);
        append(out, value, 0);
        return out.toString();
    }

    public byte[] writeUtf8(JsonValue value) {
        return write(value).getBytes(StandardCharsets.UTF_8);
    }

    private void append(StringBuilder out, JsonValue value, int depth) {
        if (value instanceof JsonObject object) {
            appendObject(out, object, depth);
        } else if (value instanceof JsonArray array) {
            appendArray(out, array, depth);
        } else if (value instanceof JsonString string) {
            escaping.appendQuoted(out, string.value());
        } else if (value instanceof JsonNumber number) {
            out.append(number.literal());
        } else if (value instanceof JsonBoolean bool) {
            out.append(bool.value() ? "true" : "false");
        } else {
            out.append("null");
        }
    }

    private void appendObject(StringBuilder out, JsonObject object, int depth) {
        boolean empty = object.members().isEmpty();
        layout.open(out, '{', depth, empty);
        boolean first = true;
        for (Map.Entry<String, JsonValue> member : object.members().entrySet()) {
            layout.separator(out, depth, first);
            first = false;
            escaping.appendQuoted(out, member.getKey());
            layout.nameSeparator(out);
            append(out, member.getValue(), depth + 1);
        }
        layout.close(out, '}', depth, empty);
    }

    private void appendArray(StringBuilder out, JsonArray array, int depth) {
        boolean empty = array.elements().isEmpty();
        layout.open(out, '[', depth, empty);
        boolean first = true;
        for (JsonValue element : array.elements()) {
            layout.separator(out, depth, first);
            first = false;
            append(out, element, depth + 1);
        }
        layout.close(out, ']', depth, empty);
    }
}
