// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record JsonObject(Map<String, JsonValue> members) implements JsonValue {

    public JsonObject {
        members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
    }

    public static JsonObjectBuilder builder() {
        return new JsonObjectBuilder();
    }

    public Optional<JsonValue> get(String name) {
        return Optional.ofNullable(members.get(name));
    }

    public boolean has(String name) {
        return members.containsKey(name);
    }

    public Optional<String> string(String name) {
        return get(name).filter(JsonString.class::isInstance).map(v -> ((JsonString) v).value());
    }

    public Optional<JsonObject> object(String name) {
        return get(name).filter(JsonObject.class::isInstance).map(JsonObject.class::cast);
    }

    public Optional<JsonArray> array(String name) {
        return get(name).filter(JsonArray.class::isInstance).map(JsonArray.class::cast);
    }

    public Optional<JsonNumber> number(String name) {
        return get(name).filter(JsonNumber.class::isInstance).map(JsonNumber.class::cast);
    }

    public Optional<Boolean> bool(String name) {
        return get(name).filter(JsonBoolean.class::isInstance).map(v -> ((JsonBoolean) v).value());
    }

    @Override
    public Object toPlain() {
        Map<String, Object> plain = new LinkedHashMap<>();
        members.forEach((key, value) -> plain.put(key, value.toPlain()));
        return plain;
    }
}
