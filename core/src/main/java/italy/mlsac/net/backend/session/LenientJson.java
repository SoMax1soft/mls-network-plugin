// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonSyntaxException;
import java.util.Optional;

public final class LenientJson {

    private LenientJson() {
    }

    public static Optional<JsonObject> object(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonParser.parseObject(text.trim()));
        } catch (JsonSyntaxException notJson) {
            return Optional.empty();
        }
    }
}
