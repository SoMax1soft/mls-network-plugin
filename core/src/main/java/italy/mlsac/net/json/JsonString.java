// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

import java.util.Objects;

public record JsonString(String value) implements JsonValue {

    public JsonString {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public Object toPlain() {
        return value;
    }
}
