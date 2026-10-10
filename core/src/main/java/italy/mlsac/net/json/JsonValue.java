// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

    Object toPlain();
}
