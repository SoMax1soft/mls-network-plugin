// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonObjectBuilder {

    private final Map<String, JsonValue> members = new LinkedHashMap<>();

    JsonObjectBuilder() {
    }

    public JsonObjectBuilder put(String name, JsonValue value) {
        members.put(name, value);
        return this;
    }

    public JsonObjectBuilder put(String name, String value) {
        return put(name, new JsonString(value));
    }

    public JsonObjectBuilder put(String name, long value) {
        return put(name, JsonNumber.of(value));
    }

    public JsonObjectBuilder put(String name, double value) {
        return put(name, JsonNumber.of(value));
    }

    public JsonObjectBuilder put(String name, boolean value) {
        return put(name, JsonBoolean.of(value));
    }

    public JsonObjectBuilder putAll(JsonObject other) {
        members.putAll(other.members());
        return this;
    }

    public JsonObject build() {
        return new JsonObject(members);
    }
}
