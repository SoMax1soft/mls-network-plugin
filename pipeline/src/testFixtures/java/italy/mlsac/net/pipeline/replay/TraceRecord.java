// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.json.JsonNumber;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class TraceRecord {
    private final Map<String, Object> fields;

    TraceRecord(Map<String, Object> fields) {
        this.fields = fields;
    }

    long sequence() {
        return ((JsonNumber) fields.get("s")).asLong();
    }

    String type() {
        return (String) fields.get("t");
    }

    String name() {
        return (String) fields.get("n");
    }

    int depth() {
        Object depth = fields.get("d");
        return depth == null ? -1 : ((JsonNumber) depth).asInt();
    }

    boolean has(String key) {
        return fields.containsKey(key);
    }

    Object raw(String key) {
        return fields.get(key);
    }

    UUID player() {
        return uuid("p");
    }

    UUID uuid(String key) {
        Object value = fields.get(key);
        return value == null ? null : UUID.fromString((String) value);
    }

    String text(String key) {
        return (String) fields.get(key);
    }

    boolean bool(String key) {
        return Boolean.TRUE.equals(fields.get(key));
    }

    long integer(String key) {
        return ((JsonNumber) fields.get(key)).asLong();
    }

    double real(String key) {
        return TraceValues.real(fields.get(key));
    }

    float single(String key) {
        return TraceValues.single(fields.get(key));
    }

    Object value() {
        return fields.get("v");
    }

    @SuppressWarnings("unchecked")
    List<Object> list(String key) {
        return (List<Object>) fields.get(key);
    }

    boolean isEvent() {
        return "E".equals(type());
    }

    boolean isRead() {
        return "R".equals(type());
    }

    boolean isOutput() {
        return "O".equals(type());
    }

    @Override
    public String toString() {
        return "s=" + sequence() + " " + type() + " " + name();
    }
}
