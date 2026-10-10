// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

public enum JsonNull implements JsonValue {
    INSTANCE;

    @Override
    public Object toPlain() {
        return null;
    }
}
