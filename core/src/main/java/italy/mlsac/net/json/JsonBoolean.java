// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

public record JsonBoolean(boolean value) implements JsonValue {

    public static final JsonBoolean TRUE = new JsonBoolean(true);
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    public static JsonBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public Object toPlain() {
        return value;
    }
}
