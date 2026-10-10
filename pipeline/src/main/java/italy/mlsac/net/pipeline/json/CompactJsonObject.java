// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

public final class CompactJsonObject {
    private final StringBuilder body = new StringBuilder("{");
    private boolean empty = true;

    public CompactJsonObject text(String key, String value) {
        return raw(key, JsonEscaping.COMPACT.quote(value));
    }

    public CompactJsonObject integer(String key, long value) {
        return raw(key, Long.toString(value));
    }

    public CompactJsonObject bool(String key, boolean value) {
        return raw(key, Boolean.toString(value));
    }

    private CompactJsonObject raw(String key, String valueText) {
        if (!empty) {
            body.append(',');
        }
        empty = false;
        body.append(JsonEscaping.COMPACT.quote(key)).append(':').append(valueText);
        return this;
    }

    public String render() {
        return body + "}";
    }
}
